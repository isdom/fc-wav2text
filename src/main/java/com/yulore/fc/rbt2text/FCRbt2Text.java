package com.yulore.fc.rbt2text;

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.Credentials;
import com.aliyun.fc.runtime.PojoRequestHandler;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.ConnectionFactory;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import static org.asynchttpclient.Dsl.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class FCRbt2Text implements PojoRequestHandler<RbtEvent[], String> {
    @Override
    public String handleRequest(final RbtEvent[] events, final Context context) {
        final AtomicReference<Throwable> exRef = new AtomicReference<>(null);

        try (final RedisClient redisClient = RedisClient.create(getRedisURI());
             final StatefulRedisConnection<String, String> redisConnection = redisClient.connect();
             final AsyncHttpClient ahc = asyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
                     .setMaxRequestRetry(0)
                     .setWebSocketMaxBufferSize(1024000)
                     .setWebSocketMaxFrameSize(1024000).build());
             final com.rabbitmq.client.Connection rabbitmqConnection = getRabbitMQConnectionFactory(context).newConnection();
             final com.rabbitmq.client.Channel rabbitmqChannel = rabbitmqConnection.createChannel()) {

            context.getLogger().warn("func: (FunctionName:" + context.getFunctionParam().getFunctionName()
                    + "/RequestId:" + context.getRequestId() +")");

            final RedisAsyncCommands<String, String> redisCommands = redisConnection.async();
            final JSONObject funcInfo = new JSONObject();
            funcInfo.put("conn", events.length);
            funcInfo.put("tm", System.currentTimeMillis());

            boolean setOk = redisCommands.hset(context.getFunctionParam().getFunctionName(),
                    context.getRequestId(),
                    funcInfo.toString()).get();
            context.getLogger().info("hset:" + context.getFunctionParam().getFunctionName() + "-" + context.getRequestId() + "/"+ funcInfo + " -> "+ setOk);

            // context.getLogger().info("handle (" + events.length + ") events");
            // context.getLogger().info("AHC config:" + ahc.getConfig());

            final CountDownLatch finishLatch = new CountDownLatch(events.length);

            // 创建OSSClient实例。
            // 获取密钥信息，执行前，确保函数所在的服务配置了角色信息，并且角色需要拥有 AliyunOSSFullAccess权限
            // 建议直接使用 AliyunFCDefaultRole 角色
            final OSS ossClient = getOssClient(context.getExecutionCredentials());

            final Consumer<RbtResultVO> sendResult = buildSendRbtResultVO(context, rabbitmqChannel);

            for (RbtEvent event : events) {
                final RbtResultVO rrvo = new RbtResultVO();
                rrvo.setSessionId(event.data.body.getSessionId());
                rrvo.setSourceTimestamp(event.data.body.getSourceTimestamp());

                final OSSObject source = getOSSObject(context, event, ossClient, rrvo);
                if (source == null) {
                    finishLatch.countDown();
                    continue;
                }
                try {
                    context.getLogger().info("oss source: " + source.getKey());
                    rrvo.setStartProcessTimestamp(System.currentTimeMillis());
                    new FunasrClient(context,
                            ahc.prepareGet(System.getenv("FUNASR_WSURI")),
                            source.getObjectContent(),
                            (text) -> {
                                rrvo.setEndProcessTimestamp(System.currentTimeMillis());
                                rrvo.setText(text);
                                try {
                                    source.close();
                                } catch (IOException e) {
                                    // throw new RuntimeException(e);
                                }
                                sendResult.accept(rrvo);
                                finishLatch.countDown();
                            },
                            (throwable) -> {
                                exRef.set(throwable);
                                rrvo.setEndProcessTimestamp(System.currentTimeMillis());
                                rrvo.setText(throwable.toString());
                                try {
                                    source.close();
                                } catch (IOException e) {
                                    // throw new RuntimeException(e);
                                }
                                sendResult.accept(rrvo);
                                finishLatch.countDown();
                            },
                            (code, reason) -> {
                                exRef.set(new RuntimeException("close:" + code + "/" + reason));
                                rrvo.setEndProcessTimestamp(System.currentTimeMillis());
                                rrvo.setText("error:close:" + code + "/" + reason);
                                try {
                                    source.close();
                                } catch (IOException e) {
                                    // throw new RuntimeException(e);
                                }
                                sendResult.accept(rrvo);
                                finishLatch.countDown();
                            });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            context.getLogger().info("wait for all funasr offline ("+events.length+") task complete");
            // wait for complete
            finishLatch.await();
            context.getLogger().info("all funasr offline ("+events.length+") task completed.");

            long delCnt = redisCommands.hdel(context.getFunctionParam().getFunctionName(),
                    context.getRequestId()).get();
            context.getLogger().info("hdel:" + context.getFunctionParam().getFunctionName() + "-" + context.getRequestId() + " -> " + delCnt);

            // 关闭OSSClient
            ossClient.shutdown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (exRef.get() != null) {
            throw new RuntimeException(exRef.get());
        }
        return "handle (" + events.length +") events";
    }

    private @NotNull Consumer<RbtResultVO> buildSendRbtResultVO(final Context context,
                                                                final com.rabbitmq.client.Channel rabbitmqChannel) {
        final String exchange= System.getenv("RBT_MQ_EXCHANGE");
        final String routingKey = System.getenv("RBT_MQ_ROUTINGKEY");

        context.getLogger().info("MQ env: exchange:" + exchange + ",routingKey:" + routingKey);
        return (vo) -> {
            try {
                sendRbtResult(context, vo, rabbitmqChannel, exchange, routingKey);
            } catch (IOException e) {
                // throw new RuntimeException(e);
            }
        };
    }

    private static @NotNull RedisURI getRedisURI() {
        return RedisURI.Builder.redis(System.getenv("RBT_REDIS_HOST"))
                .withPassword(System.getenv("RBT_REDIS_PASSWD").toCharArray())
                .withPort(Integer.parseInt(System.getenv("RBT_REDIS_PORT")))
                .withDatabase(Integer.parseInt(System.getenv("RBT_REDIS_DB")))
                .build();
    }

    private void sendRbtResult(final Context context,
                               final RbtResultVO vo,
                               final com.rabbitmq.client.Channel rabbitmqChannel,
                               final String exchange,
                               final String routingKey) throws IOException {
        final String json = new ObjectMapper().writeValueAsString(vo);
        rabbitmqChannel.basicPublish(exchange, routingKey, null, json.getBytes(StandardCharsets.UTF_8));
        context.getLogger().info("send to rabbitmq:" + json);
    }

    private static @NotNull ConnectionFactory getRabbitMQConnectionFactory(final Context context) {
        final ConnectionFactory factory = new ConnectionFactory();
        final String uri = System.getenv("RBT_MQ_URI");
        try {
            factory.setUri(uri);
            context.getLogger().info("MQ env: uri:" + uri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return factory;
    }

    private static OSS getOssClient(final Credentials creds) {
        final String endpoint = System.getenv("RBT_OSS_ENDPOINT");
        return new OSSClientBuilder().build(endpoint,
                creds.getAccessKeyId(),
                creds.getAccessKeySecret(),
                creds.getSecurityToken());
    }

    private OSSObject getOSSObject(final Context context, final RbtEvent event, final OSS ossClient, final RbtResultVO resultvo) {
        if (event.data == null || event.data.body == null || event.data.body.ossPath == null) {
            context.getLogger().warn("event's ossPath is null, skip");
            return null;
        }

        // vfs://{vfs=vfs_oss,uuid={uuid},bucket=ylhz-aicall}public/rbtrec-dev/291/20240813/202408130000430000314462.wav
        int bucketBeginIdx = event.data.body.ossPath.indexOf("bucket=");
        if (bucketBeginIdx == -1) {
            context.getLogger().warn("event's ossPath(" + event.data.body.ossPath + ") NOT contains bucket, skip");
            return null;
        }

        int bucketEndIdx = event.data.body.ossPath.indexOf('}', bucketBeginIdx);
        if (bucketEndIdx == -1) {
            context.getLogger().warn("event's ossPath(" + event.data.body.ossPath + ") missing '}', skip");
            return null;
        }

        final String bucketName = event.data.body.ossPath.substring(bucketBeginIdx + 7, bucketEndIdx);
        final String objectName = event.data.body.ossPath.substring(bucketEndIdx + 1);

        if (!ossClient.doesObjectExist(bucketName, objectName)) {
            context.getLogger().warn("event's ossPath(" + event.data.body.ossPath + ") !NOT! exist, skip");
            return null;
        }

        resultvo.setObjectName(objectName);
        return ossClient.getObject(bucketName, objectName);
    }
}