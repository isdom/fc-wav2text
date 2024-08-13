package com.yulore.fc.rbt2text;

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.Credentials;
import com.aliyun.fc.runtime.PojoRequestHandler;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
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

    private void sendRbtResult(Context context, final RbtResultVO vo) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        final ConnectionFactory factory = new ConnectionFactory();
        final String uri = System.getenv("RBT_MQ_URI");
        final String exchange= System.getenv("RBT_MQ_EXCHANGE");
        final String routingKey = System.getenv("RBT_MQ_ROUTINGKEY");
        factory.setUri(uri);
        try (final Connection conn = factory.newConnection();
             final Channel channel = conn.createChannel()) {
            final String json = new ObjectMapper().writeValueAsString(vo);
            context.getLogger().info("sendRbtResult:" + json +",uri:" + uri + ", exchange:" + exchange + ",routingKey:" + routingKey);
            channel.basicPublish(exchange, routingKey, null,
                    json.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public String handleRequest(final RbtEvent[] events, final Context context) {
        final String endpoint = System.getenv("RBT_OSS_ENDPOINT");

        // 获取密钥信息，执行前，确保函数所在的服务配置了角色信息，并且角色需要拥有 AliyunOSSFullAccess权限
        // 建议直接使用 AliyunFCDefaultRole 角色
        final Credentials creds = context.getExecutionCredentials();

        // 创建OSSClient实例。
        final OSS ossClient = new OSSClientBuilder().build(endpoint,
                                creds.getAccessKeyId(),
                                creds.getAccessKeySecret(),
                                creds.getSecurityToken());
        String text = "";
        for (RbtEvent event : events) {
            if (event.data == null || event.data.body == null || event.data.body.ossPath == null) {
                context.getLogger().warn("event's ossPath is null, skip");
                continue;
            }

            // vfs://{vfs=vfs_oss,uuid={uuid},bucket=ylhz-aicall}public/rbtrec-dev/291/20240813/202408130000430000314462.wav
            int bucketBeginIdx = event.data.body.ossPath.indexOf("bucket=");
            if (bucketBeginIdx == -1) {
                context.getLogger().warn("event's ossPath(" + event.data.body.ossPath + ") NOT contains bucket, skip");
                continue;
            }

            int bucketEndIdx = event.data.body.ossPath.indexOf('}', bucketBeginIdx);
            if (bucketEndIdx == -1) {
                context.getLogger().warn("event's ossPath(" + event.data.body.ossPath + ") missing '}', skip");
                continue;
            }

            final String bucketName = event.data.body.ossPath.substring(bucketBeginIdx + 7, bucketEndIdx);
            final String objectName = event.data.body.ossPath.substring(bucketEndIdx + 1);

            if (!ossClient.doesObjectExist(bucketName, objectName)) {
                context.getLogger().warn("event's ossPath(" + event.data.body.ossPath + ") !NOT! exist, skip");
                continue;
            }

            final RbtResultVO vo = new RbtResultVO();
            vo.setSessionId(event.data.body.getSessionId());
            vo.setObjectName(objectName);
            vo.setSourceTimestamp(event.data.body.getSourceTimestamp());

            try (final OSSObject ossObj = ossClient.getObject(bucketName, objectName)) {
                context.getLogger().info("ossobj: " + ossObj.getKey());
                try {
                    vo.setStartProcessTimestamp(System.currentTimeMillis());
                    text = FunasrWsClient.wav2text(System.getenv("FUNASR_WSURI"), ossObj.getObjectContent(), context);
                    vo.setEndProcessTimestamp(System.currentTimeMillis());
                    vo.setText(text);
                    sendRbtResult(context, vo);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (KeyManagementException e) {
                    throw new RuntimeException(e);
                } catch (TimeoutException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (OSSException e) {
                throw new RuntimeException(e);
            } catch (ClientException e) {
                throw new RuntimeException(e);
            }
        }
        // 关闭OSSClient
        ossClient.shutdown();
        return text;
    }
}