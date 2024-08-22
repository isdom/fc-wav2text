package com.yulore.fc.rbt2text;

import com.yulore.fc.pojo.RabbitMQData;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class RbtData extends RabbitMQData {
    @Setter
    @Getter
    @ToString
    static class Body {
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

    @Override
    public String toString() {
        return "RbtData{" +
                "body=" + body +
                "} " + super.toString();
    }
}
