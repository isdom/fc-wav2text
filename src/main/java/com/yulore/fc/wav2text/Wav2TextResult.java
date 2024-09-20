package com.yulore.fc.wav2text;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Wav2TextResult {
    String status;
    String audio_text;
    String audio_id;
}
