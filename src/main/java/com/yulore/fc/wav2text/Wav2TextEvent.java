package com.yulore.fc.wav2text;

import com.yulore.fc.pojo.CloudEvent;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Wav2TextEvent extends CloudEvent {
    Wav2TextData data;
}
