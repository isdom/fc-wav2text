package com.yulore.fc.wav2text;

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.Credentials;
import com.aliyun.fc.runtime.PojoRequestHandler;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.yulore.fc.event.http.HttpRequestVO;
import com.yulore.fc.event.http.HttpResponseVO;
import com.yulore.fc.http.MultipartParser;
import com.yulore.fc.wav2text.vo.ResultVO;
import com.yulore.fc.wav2text.vo.Wav2TextEvent;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.XSlf4j;
import org.apache.commons.fileupload.FileUploadException;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.asynchttpclient.Dsl.asyncHttpClient;

@Slf4j
public class Wav2TextHttpServer implements PojoRequestHandler<HttpRequestVO, HttpResponseVO> {
    @Override
    public HttpResponseVO handleRequest(final HttpRequestVO httpRequest, final Context context) {

        log.info("IsBase64Encoded: {}", httpRequest.getIsBase64Encoded());
        log.info("headers: {}", httpRequest.getHeaders());

        try {
            byte[] body = Base64.getDecoder().decode(httpRequest.getBody());
            byte[] fileBytes  = MultipartParser.parseRequest(body, httpRequest.getHeaders().get("Content-Type")).get("file").get(0).get();
            log.info("fileBytes: {} bytes", fileBytes.length);
        } catch (FileUploadException ex) {
            throw new RuntimeException(ex);
        }

        final HttpResponseVO httpResponse = new HttpResponseVO();
        httpResponse.setStatusCode(200);
        return httpResponse;
    }
}