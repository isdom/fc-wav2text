package com.yulore.fc.rbt2text;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class RbtResultVO {
    String sessionId;
    String objectName;
    String text;
    long   sourceTimestamp;
    long   startProcessTimestamp;
    long   endProcessTimestamp;
}
