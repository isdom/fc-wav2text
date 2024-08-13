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
package com.yulore.fc.rbt2text;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This example demonstrates how to connect to websocket server. */
public class FunasrWsClient extends WebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(FunasrWsClient.class);

    public FunasrWsClient(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public FunasrWsClient(URI serverURI) {

        super(serverURI);
    }

    public FunasrWsClient(URI serverUri, Map<String, String> httpHeaders) {
        super(serverUri, httpHeaders);
    }

    public void getSslContext(String keyfile, String certfile) {
        // TODO
        return;
    }

    // send json at first time
    public void sendJson(
            String mode,
            String strChunkSize,
            int chunkInterval,
            String wavName,
            boolean isSpeaking,
            String suffix) {
        try {

            JSONObject obj = new JSONObject();
            obj.put("mode", mode);
            JSONArray array = new JSONArray();
            String[] chunkList = strChunkSize.split(",");
            for (int i = 0; i < chunkList.length; i++) {
                array.add(Integer.valueOf(chunkList[i].trim()));
            }

            obj.put("chunk_size", array);
            obj.put("chunk_interval", chunkInterval);
            obj.put("wav_name", wavName);

//            if(suffix.equals("wav")){
//                suffix="pcm";
//            }
            obj.put("wav_format", suffix);
            if (isSpeaking) {
                obj.put("is_speaking", true);
            } else {
                obj.put("is_speaking", false);
            }
            logger.info("sendJson: " + obj);
            send(obj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // send json at end of wav
    public void sendEof() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("is_speaking", false);

            logger.info("sendEof: " + obj);

            send(obj.toString());
            iseof = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // function for rec wav file
    public void sendwav(
            String mode,
            String strChunkSize,
            int chunkInterval,
            int chunkSize,
            InputStream is) throws IOException, InterruptedException {
        sendJson(mode, strChunkSize, chunkInterval, wavName, true, "wav");

        byte[] bytes = new byte[chunkSize];

        int readSize = 0;
//            if (FunasrWsClient.wavPath.endsWith(".wav")) {
//                fis.read(bytes, 0, 44); //skip first 44 wav header
//            }
        readSize = is.read(bytes, 0, chunkSize);
        while (readSize > 0) {
            // send when it is chunk size
            if (readSize == chunkSize) {
                send(bytes); // send buf to server

            } else {
                // send when at last or not is chunk size
                byte[] tmpBytes = new byte[readSize];
                for (int i = 0; i < readSize; i++) {
                    tmpBytes[i] = bytes[i];
                }
                send(tmpBytes);
            }
            // if not in offline mode, we simulate online stream by sleep
            if (!mode.equals("offline")) {
                Thread.sleep(Integer.valueOf(chunkSize / 32));
            }

            readSize = is.read(bytes, 0, chunkSize);
        }

        if (!mode.equals("offline")) {
            // if not offline, we send eof and wait for 3 seconds to close
            Thread.sleep(2000);
            sendEof();
            Thread.sleep(3000);
            close();
        } else {
            // if offline, just send eof
            sendEof();
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
    }

    @Override
    public void onMessage(String message) {
        JSONObject jsonObject = new JSONObject();
        JSONParser jsonParser = new JSONParser();
        logger.info("received: " + message);
        try {
            jsonObject = (JSONObject) jsonParser.parse(message);
            logger.info("text: " + jsonObject.get("text"));
            text = jsonObject.get("text").toString();
            if(jsonObject.containsKey("timestamp"))
            {
                logger.info("timestamp: " + jsonObject.get("timestamp"));
            }
        } catch (org.json.simple.parser.ParseException e) {
            e.printStackTrace();
        }
        if (iseof && mode.equals("offline") && !jsonObject.containsKey("is_final")) {
            close();
        }

        if (iseof && mode.equals("offline") && jsonObject.containsKey("is_final") && jsonObject.get("is_final").equals("false")) {
            close();
        }
        finishLatch.countDown();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

        logger.info(
                "Connection closed by "
                        + (remote ? "remote peer" : "us")
                        + " Code: "
                        + code
                        + " Reason: "
                        + reason);
    }

    @Override
    public void onError(Exception ex) {
        logger.info("ex: " + ex);
        ex.printStackTrace();
        // if the error is fatal then onClose will be called additionally
    }

    private boolean iseof = false;
    private CountDownLatch finishLatch = new CountDownLatch(1);;
    private String text;
    static String mode = "online";
    static String strChunkSize = "5,10,5";
    static int chunkInterval = 10;

    static String hotwords="";
    static String fsthotwords="";

    String wavName = "javatest";

    public static String wav2text(String wsuri, InputStream is) throws InterruptedException, URISyntaxException, IOException {
        final FunasrWsClient c = new FunasrWsClient(new URI(wsuri));

        if (!c.connectBlocking()) {
            return "";
        }
        int RATE = 8000;
        String[] chunkList = strChunkSize.split(",");
        int int_chunk_size = 60 * Integer.valueOf(chunkList[1].trim()) / chunkInterval;
        int CHUNK = Integer.valueOf(RATE / 1000 * int_chunk_size);
        int stride =
                Integer.valueOf(
                        60 * Integer.valueOf(chunkList[1].trim()) / chunkInterval / 1000 * RATE * 2);
        System.out.println("chunk_size:" + String.valueOf(int_chunk_size));
        System.out.println("CHUNK:" + CHUNK);
        System.out.println("stride:" + String.valueOf(stride));
        int sendChunkSize = CHUNK * 2;

        c.sendwav("offline", strChunkSize, chunkInterval, sendChunkSize, is);
        c.finishLatch.await();
        return c.text;
    }
}