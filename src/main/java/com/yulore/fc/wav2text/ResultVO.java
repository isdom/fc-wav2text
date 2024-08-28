package com.yulore.fc.wav2text;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ResultVO {
    String sessionId;
    String objectName;
    String text;
    long   sourceTimestamp;
    long   startProcessTimestamp;
    long   endProcessTimestamp;
}
