package com.yulore.demo;

import com.yulore.fc.wav2text.FunasrClient;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import static org.asynchttpclient.Dsl.asyncHttpClient;

public class Wav2TextDemo  {
    public static void main(final String[] args) throws IOException {
        try (final AsyncHttpClient ahc = asyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
                .setMaxRequestRetry(0)
                .setWebSocketMaxBufferSize(1024000)
                .setWebSocketMaxFrameSize(1024000).build());
             final InputStream is = new FileInputStream(args[0])) {

            final CountDownLatch finishLatch = new CountDownLatch(1);
            new FunasrClient(null,
                    ahc.prepareGet(System.getenv("FUNASR_WSURI")),
                    "wav",
                    is,
                    (text) -> {
                        System.out.println(text);
                        finishLatch.countDown();
                    },
                    (throwable) -> {
                        finishLatch.countDown();
                    },
                    (code, reason) -> {
                        finishLatch.countDown();
                    });
            finishLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}