package com.yulore.fc.wav2text.vo;

import com.yulore.fc.event.CloudEvent;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Wav2TextEvent extends CloudEvent {
    Wav2TextData data;
}
