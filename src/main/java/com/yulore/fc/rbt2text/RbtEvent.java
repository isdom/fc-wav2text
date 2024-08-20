package com.yulore.fc.rbt2text;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yulore.fc.pojo.CloudEvent;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class RbtEvent extends CloudEvent {
    RbtData data;
}
