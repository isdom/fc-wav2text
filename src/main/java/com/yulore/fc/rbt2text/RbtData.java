package com.yulore.fc.rbt2text;

import com.yulore.fc.pojo.RabbitMQData;

public class RbtData extends RabbitMQData {
    static class Body {
        String sessionId;
        String ossPath;
        Long   sourceTimestamp;

        public Long getSourceTimestamp() {
            return sourceTimestamp;
        }

        public void setSourceTimestamp(Long sourceTimestamp) {
            this.sourceTimestamp = sourceTimestamp;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getOssPath() {
            return ossPath;
        }

        public void setOssPath(String ossPath) {
            this.ossPath = ossPath;
        }

        @Override
        public String toString() {
            return "Body{" +
                    "sessionId='" + sessionId + '\'' +
                    ", ossPath='" + ossPath + '\'' +
                    ", sourceTimestamp=" + sourceTimestamp +
                    '}';
        }
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
