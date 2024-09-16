//
// Copyright FunASR (https://github.com/alibaba-damo-academy/FunASR). All Rights
// Reserved. MIT License  (https://opensource.org/licenses/MIT)
//
/*
 * // 2022-2023 by zhaomingwork@qq.com
 */
// java FunasrWsClient
// usage:  FunasrWsClient [-h] [--port PORT] [--host HOST] [--audio_in AUDIO_IN] [--num_threads NUM_THREADS]
//                 [--chunk_size CHUNK_SIZE] [--chunk_interval CHUNK_INTERVAL] [--mode MODE]
package com.yulore.fc.wav2text;

import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** This example demonstrates how to connect to websocket server. */
@Slf4j
public class FunasrClient {

    public FunasrClient(final BoundRequestBuilder brb,
                        final String format,
                        final InputStream is,
                        final Consumer<String> onResult,
                        final Consumer<Throwable> onError,
                        final BiConsumer<Integer, String> onClose) {
        final AtomicBoolean isOnTextOrOnError = new AtomicBoolean(false);
        final AtomicReference<WebSocket> wsRef = new AtomicReference<>(null);
        brb.execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(new WebSocketListener() {
            @Override
            public void onOpen(final WebSocket ws) {
                wsRef.set(ws);

                // step 1
                sendBeginMsg(ws, format);

                // step 2
                try {
                    sendData(ws, getSendChunkSize(), is);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // step 3
                sendEof(ws);
            }

            @Override
            public void onClose(WebSocket webSocket, int code, String reason) {
                log.info("Connection closed / Code: {} Reason: {}", code, reason);
                if (!isOnTextOrOnError.get()) {
                    onClose.accept(code, reason);
                }
            }

            @Override
            public void onError(final Throwable throwable) {
                isOnTextOrOnError.compareAndExchange(false, true);
                log.info("ex: {}", String.valueOf(throwable));
                if (wsRef.get() != null) {
                    wsRef.get().sendCloseFrame(1000, "ex:" + throwable);
                }
                onError.accept(throwable);
            }

            @Override
            public void onTextFrame(String payload, boolean finalFragment, int rsv) {
                isOnTextOrOnError.compareAndExchange(false, true);
                JSONObject jsonObject = new JSONObject();
                final JSONParser jsonParser = new JSONParser();
                log.info("received: {}", payload);
                try {
                    jsonObject = (JSONObject) jsonParser.parse(payload);
                    log.info("text: {}", jsonObject.get("text"));
                    if (jsonObject.containsKey("timestamp")) {
                        //if (null != logger) {
                            log.info("timestamp: " + jsonObject.get("timestamp"));
                        //}
                    }
                } catch (org.json.simple.parser.ParseException e) {
                    log.error("error:" + e.toString());
                }
                if (wsRef.get() != null) {
                    wsRef.get().sendCloseFrame();
                }
                onResult.accept(jsonObject.get("text").toString());
            }
        }).build());
    }

    private int getSendChunkSize() {
        int RATE = 8000;
        String[] chunkList = strChunkSize.split(",");
        int int_chunk_size = 60 * Integer.parseInt(chunkList[1].trim()) / chunkInterval;
        int CHUNK = RATE / 1000 * int_chunk_size;
        int stride = 60 * Integer.parseInt(chunkList[1].trim()) / chunkInterval / 1000 * RATE * 2;
        //if (null != logger) {
        log.info("chunk_size:" + String.valueOf(int_chunk_size));
        log.info("CHUNK:" + CHUNK);
        log.info("stride:" + String.valueOf(stride));
        //}
        return CHUNK * 2;
    }

    private void sendBeginMsg(final WebSocket ws, final String format) {
        final JSONObject obj = new JSONObject();
        obj.put("mode", "offline");
        JSONArray array = new JSONArray();
        final String[] chunkList = strChunkSize.split(",");
        for (int i = 0; i < chunkList.length; i++) {
            array.add(Integer.valueOf(chunkList[i].trim()));
        }

        obj.put("chunk_size", array);
        obj.put("chunk_interval", chunkInterval);
        obj.put("wav_name", "ahc");

        obj.put("wav_format", format);
        obj.put("is_speaking", true);

        //if (null != logger) {
        log.info("sendJson: {}", obj);
        //}
        ws.sendTextFrame(obj.toString());
    }

    // send json at end of wav
    public void sendEof(final WebSocket ws) {
        final JSONObject obj = new JSONObject();
        obj.put("is_speaking", false);
        log.info("sendEof: {}", obj);
        ws.sendTextFrame(obj.toString());
    }

    // function for rec wav file
    public void sendData(final WebSocket ws, int chunkSize, InputStream is) throws IOException {
        final byte[] bytes = new byte[chunkSize];
        int readSize = is.read(bytes, 0, chunkSize);
        while (readSize > 0) {
            // send when it is chunk size
            if (readSize == chunkSize) {
                ws.sendBinaryFrame(bytes); // send buf to server

            } else {
                // send when at last or not is chunk size
                byte[] tmpBytes = new byte[readSize];
                System.arraycopy(bytes, 0, tmpBytes, 0, readSize);
                ws.sendBinaryFrame(tmpBytes);
            }
            readSize = is.read(bytes, 0, chunkSize);
        }
    }

    static String strChunkSize = "5,10,5";
    static int chunkInterval = 10;
}