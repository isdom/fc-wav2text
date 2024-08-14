package com.yulore.fc.rbt2text;

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.Credentials;
import com.aliyun.fc.runtime.PojoRequestHandler;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.asynchttpclient.AsyncHttpClient;

import static org.asynchttpclient.Dsl.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public class FCRbt2Text implements PojoRequestHandler<RbtEvent[], String> {

    private void sendRbtResult(final Context context, final RbtResultVO vo, Channel channel, final String exchange, final String routingKey) throws IOException {
        final String json = new ObjectMapper().writeValueAsString(vo);
        channel.basicPublish(exchange, routingKey, null, json.getBytes(StandardCharsets.UTF_8));
        context.getLogger().info("send to mq:" + json);
    }

    @Override
    public String handleRequest(final RbtEvent[] events, final Context context) {
        final String endpoint = System.getenv("RBT_OSS_ENDPOINT");

        // 获取密钥信息，执行前，确保函数所在的服务配置了角色信息，并且角色需要拥有 AliyunOSSFullAccess权限
        // 建议直接使用 AliyunFCDefaultRole 角色
        final Credentials creds = context.getExecutionCredentials();

        final ConnectionFactory factory = new ConnectionFactory();
        final String uri = System.getenv("RBT_MQ_URI");
        final String exchange= System.getenv("RBT_MQ_EXCHANGE");
        final String routingKey = System.getenv("RBT_MQ_ROUTINGKEY");
        try {
            factory.setUri(uri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try (final AsyncHttpClient ahc = asyncHttpClient();
             final Connection conn = factory.newConnection();
             final Channel channel = conn.createChannel()) {
            context.getLogger().info("AHC config:" + ahc.getConfig());
            context.getLogger().info("MQ env: uri:" + uri + ", exchange:" + exchange + ",routingKey:" + routingKey);
            // 创建OSSClient实例。
            final OSS ossClient = new OSSClientBuilder().build(endpoint,
                    creds.getAccessKeyId(),
                    creds.getAccessKeySecret(),
                    creds.getSecurityToken());
            context.getLogger().info("handle (" + events.length + ") events");

            final CountDownLatch finishLatch = new CountDownLatch(events.length);

            for (RbtEvent event : events) {
                final RbtResultVO resultvo = new RbtResultVO();
                resultvo.setSessionId(event.data.body.getSessionId());

                final OSSObject source = getOSSObject(context, event, ossClient, resultvo);
                if (source == null) {
                    finishLatch.countDown();
                    continue;
                }
                try {
                    context.getLogger().info("oss source: " + source.getKey());
                    resultvo.setStartProcessTimestamp(System.currentTimeMillis());
                    new FunasrClient(context,
                            ahc.prepareGet(System.getenv("FUNASR_WSURI")),
                            source.getObjectContent(),
                            (text) -> {
                                resultvo.setEndProcessTimestamp(System.currentTimeMillis());
                                resultvo.setText(text);
                                try {
                                    source.close();
                                } catch (IOException e) {
                                    // throw new RuntimeException(e);
                                }
                                try {
                                    sendRbtResult(context, resultvo, channel, exchange, routingKey);
                                } catch (IOException e) {
                                    // throw new RuntimeException(e);
                                }
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

            // 关闭OSSClient
            ossClient.shutdown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return "handle (" + events.length +") events";
    }

    private OSSObject getOSSObject(Context context, RbtEvent event, OSS ossClient, RbtResultVO resultvo) {
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