package com.yulore.fc.wav2text.vo;

import com.yulore.fc.event.RabbitMQData;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Wav2TextData extends RabbitMQData {
    @Data
    @ToString
    static public class Body {
        String sessionId;
        String ossPath;
        Integer stage; // 0 -- rbt , 1 -- answered
        Long   sourceTimestamp;
    }

    Body body;
}
