package com.yulore.fc.rbt2text;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yulore.fc.pojo.CloudEvent;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class RbtEvent extends CloudEvent {
    RbtData data;
}
