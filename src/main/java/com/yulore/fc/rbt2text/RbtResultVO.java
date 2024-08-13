package com.yulore.fc.rbt2text;

public class RbtResultVO {
    String sessionId;
    String objectName;
    String text;
    long   sourceTimestamp;
    long   startProcessTimestamp;
    long   endProcessTimestamp;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getSourceTimestamp() {
        return sourceTimestamp;
    }

    public void setSourceTimestamp(long sourceTimestamp) {
        this.sourceTimestamp = sourceTimestamp;
    }

    public long getStartProcessTimestamp() {
        return startProcessTimestamp;
    }

    public void setStartProcessTimestamp(long startProcessTimestamp) {
        this.startProcessTimestamp = startProcessTimestamp;
    }

    public long getEndProcessTimestamp() {
        return endProcessTimestamp;
    }

    public void setEndProcessTimestamp(long endProcessTimestamp) {
        this.endProcessTimestamp = endProcessTimestamp;
    }

    @Override
    public String toString() {
        return "RbtResultVO{" +
                "sessionId='" + sessionId + '\'' +
                ", objectName='" + objectName + '\'' +
                ", text='" + text + '\'' +
                ", sourceTimestamp=" + sourceTimestamp +
                ", startProcessTimestamp=" + startProcessTimestamp +
                ", endProcessTimestamp=" + endProcessTimestamp +
                '}';
    }
}
