package com.yulore.fc.wav2text;

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.PojoRequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yulore.fc.event.http.HttpRequestVO;
import com.yulore.fc.event.http.HttpResponseVO;
import com.yulore.fc.http.MultipartParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.asynchttpclient.Dsl.asyncHttpClient;

@Slf4j
public class Wav2TextHttpServer implements PojoRequestHandler<HttpRequestVO, HttpResponseVO> {
    @Override
    public HttpResponseVO handleRequest(final HttpRequestVO httpRequest, final Context context) {

        log.info("IsBase64Encoded: {}", httpRequest.getIsBase64Encoded());
        log.info("headers: {}", httpRequest.getHeaders());
        log.info("http: {}", httpRequest.getRequestContext().getHttp());
        if (!httpRequest.getRequestContext().getHttp().getMethod().equals("POST")) {
            final HttpResponseVO responseVO = new HttpResponseVO();
            responseVO.setStatusCode(200);
            return responseVO;
        }
        final byte[] bodyBytes = Base64.getDecoder().decode(httpRequest.getBody());
        final Map<String, List<FileItem>> multipart;
        try {
            multipart = MultipartParser.parseRequest(bodyBytes, httpRequest.getHeaders().get("Content-Type"));
        } catch (FileUploadException e) {
            throw new RuntimeException(e);
        }
        final String audioId = getItemAsString(multipart, "audio_id");
        final String timeStamp = getItemAsString(multipart, "timestamp");
        final String sign = getItemAsString(multipart, "sign");
        final FileItem file  = multipart.get("file").get(0);
        log.info("ASR params => audio_id: {}, timestamp: {}, sign: {}, file length: {}", audioId, timeStamp, sign, file.get().length);

        final AtomicReference<String> refText = new AtomicReference<>(null);
        final AtomicReference<String> refStatus = new AtomicReference<>(null);

        try (final AsyncHttpClient ahc = asyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
                     .setMaxRequestRetry(0)
                     .setWebSocketMaxBufferSize(1024000)
                     .setWebSocketMaxFrameSize(1024000).build());
        ) {
            final CountDownLatch finishLatch = new CountDownLatch(1);
            new FunasrClient(ahc.prepareGet(System.getenv("FUNASR_WSURI")),
                    "wav",
                    // source.getObjectContent(),
                    file.getInputStream(),
                    (text) -> {
                        refText.set(text);
                        refStatus.set("0");
                        finishLatch.countDown();
                    },
                    (throwable) -> {
                        refText.set(throwable.toString());
                        refStatus.set("-1");
                        finishLatch.countDown();
                    },
                    (code, reason) -> {
                        refText.set(code + "/" + reason);
                        refStatus.set("-1");
                        finishLatch.countDown();
                    });
            finishLatch.await();
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }

        final HttpResponseVO httpResponse = new HttpResponseVO();
        httpResponse.setStatusCode(200);

        final Wav2TextResult result = new Wav2TextResult();
        result.setStatus(refStatus.get());
        result.setAudio_text(refText.get());
        result.setAudio_id(audioId);
        try {
            httpResponse.setBody(new ObjectMapper().writeValueAsString(result));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return httpResponse;
    }

    private static String getItemAsString(final Map<String, List<FileItem>> multipart, final String fieldName) {
        final List<FileItem> items = multipart.get(fieldName);
        if (items == null || items.isEmpty()) {
            return null;
        }
        return items.get(0).getString();
    }
}