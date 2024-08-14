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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

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
        try (final Connection conn = factory.newConnection();
             final Channel channel = conn.createChannel()) {
            context.getLogger().info("MQ env: uri:" + uri + ", exchange:" + exchange + ",routingKey:" + routingKey);
            // 创建OSSClient实例。
            final OSS ossClient = new OSSClientBuilder().build(endpoint,
                    creds.getAccessKeyId(),
                    creds.getAccessKeySecret(),
                    creds.getSecurityToken());
            context.getLogger().info("handle (" + events.length + ") events");
            for (RbtEvent event : events) {
                try {
                    RbtResultVO vo = rbt2text_by_funasr(context, event, ossClient);
                    if (vo != null) {
                        sendRbtResult(context, vo, channel, exchange, routingKey);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            // 关闭OSSClient
            ossClient.shutdown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "handle (" + events.length +") events";
    }

    private RbtResultVO rbt2text_by_funasr(final Context context, final RbtEvent event, final OSS ossClient) throws IOException, URISyntaxException, InterruptedException, NoSuchAlgorithmException, KeyManagementException, TimeoutException {
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

        final RbtResultVO vo = new RbtResultVO();
        vo.setSessionId(event.data.body.getSessionId());
        vo.setObjectName(objectName);
        vo.setSourceTimestamp(event.data.body.getSourceTimestamp());

        try (final OSSObject ossObj = ossClient.getObject(bucketName, objectName)) {
            context.getLogger().info("ossobj: " + ossObj.getKey());
            vo.setStartProcessTimestamp(System.currentTimeMillis());
            final String text = FunasrWsClient.wav2text(context, System.getenv("FUNASR_WSURI"), ossObj.getObjectContent());
            vo.setEndProcessTimestamp(System.currentTimeMillis());
            vo.setText(text);
        }

        return vo;
    }
}