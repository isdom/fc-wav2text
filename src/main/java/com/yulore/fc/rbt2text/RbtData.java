package com.yulore.fc.rbt2text;

import com.yulore.fc.pojo.RabbitMQData;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.asynchttpclient.request.body.Body;

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

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }
}
