package com.yulore.fc.rbt2text;

import com.yulore.fc.pojo.RabbitMQData;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.asynchttpclient.request.body.Body;

@Data
@ToString
public class RbtData extends RabbitMQData {
    @Data
    @ToString
    static public class Body {
        String sessionId;
        String ossPath;
        Long   sourceTimestamp;
    }

    Body body;
}
