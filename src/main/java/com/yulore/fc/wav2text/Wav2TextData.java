package com.yulore.fc.wav2text;

import com.yulore.fc.pojo.RabbitMQData;
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
        Long   sourceTimestamp;
    }

    Body body;
}
