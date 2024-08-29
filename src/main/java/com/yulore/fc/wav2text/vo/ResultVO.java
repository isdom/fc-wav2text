package com.yulore.fc.wav2text.vo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ResultVO {
    String sessionId;
    Integer stage;
    String objectName;
    String text;
    long   sourceTimestamp;
    long   startProcessTimestamp;
    long   endProcessTimestamp;
}
