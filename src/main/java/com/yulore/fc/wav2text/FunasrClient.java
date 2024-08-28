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

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.FunctionComputeLogger;
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
public class FunasrClient {

    FunctionComputeLogger logger;

    public FunasrClient(final Context context,
                        final BoundRequestBuilder brb,
                        final InputStream is,
                        final Consumer<String> onResult,
                        final Consumer<Throwable> onError,
                        final BiConsumer<Integer, String> onClose) {
        logger = context.getLogger();
        final AtomicBoolean isOnTextOrOnError = new AtomicBoolean(false);
        final AtomicReference<WebSocket> wsRef = new AtomicReference<>(null);
        brb.execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(new WebSocketListener() {
            @Override
            public void onOpen(final WebSocket ws) {
                wsRef.set(ws);

                // step 1
                sendBeginMsg(ws);

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
                logger.info("Connection closed / Code: " + code + " Reason: " + reason);
                if (!isOnTextOrOnError.get()) {
                    onClose.accept(code, reason);
                }
            }

            @Override
            public void onError(final Throwable throwable) {
                isOnTextOrOnError.compareAndExchange(false, true);
                logger.info("ex: " + throwable);
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
                logger.info("received: " + payload);
                try {
                    jsonObject = (JSONObject) jsonParser.parse(payload);
                    logger.info("text: " + jsonObject.get("text"));
                    if (jsonObject.containsKey("timestamp")) {
                        logger.info("timestamp: " + jsonObject.get("timestamp"));
                    }
                } catch (org.json.simple.parser.ParseException e) {
                    e.printStackTrace();
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
        int int_chunk_size = 60 * Integer.valueOf(chunkList[1].trim()) / chunkInterval;
        int CHUNK = Integer.valueOf(RATE / 1000 * int_chunk_size);
        int stride =
                Integer.valueOf(
                        60 * Integer.valueOf(chunkList[1].trim()) / chunkInterval / 1000 * RATE * 2);
        logger.info("chunk_size:" + String.valueOf(int_chunk_size));
        logger.info("CHUNK:" + CHUNK);
        logger.info("stride:" + String.valueOf(stride));
        int sendChunkSize = CHUNK * 2;
        return sendChunkSize;
    }

    private void sendBeginMsg(final WebSocket ws) {
        JSONObject obj = new JSONObject();
        obj.put("mode", "offline");
        JSONArray array = new JSONArray();
        String[] chunkList = strChunkSize.split(",");
        for (int i = 0; i < chunkList.length; i++) {
            array.add(Integer.valueOf(chunkList[i].trim()));
        }

        obj.put("chunk_size", array);
        obj.put("chunk_interval", chunkInterval);
        obj.put("wav_name", "ahc");

        obj.put("wav_format", "wav");
        obj.put("is_speaking", true);

        logger.info("sendJson: " + obj);
        ws.sendTextFrame(obj.toString());
    }

    // send json at end of wav
    public void sendEof(final WebSocket ws) {
        JSONObject obj = new JSONObject();
        obj.put("is_speaking", false);
        logger.info("sendEof: " + obj);
        ws.sendTextFrame(obj.toString());
    }

    // function for rec wav file
    public void sendData(final WebSocket ws, int chunkSize, InputStream is) throws IOException {
        byte[] bytes = new byte[chunkSize];

        int readSize = 0;
        readSize = is.read(bytes, 0, chunkSize);
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