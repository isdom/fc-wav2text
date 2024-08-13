package com.yulore.fc.rbt2text;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yulore.fc.pojo.CloudEvent;

public class RbtEvent extends CloudEvent {
    RbtData data;

    @JsonProperty("data")
    public RbtData getData() {
        return data;
    }

    @JsonProperty("data")
    public void setData(RbtData data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "RbtEvent{" +
                "data=" + data +
                "} " + super.toString();
    }
}
