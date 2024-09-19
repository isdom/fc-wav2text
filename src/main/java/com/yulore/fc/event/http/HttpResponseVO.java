package com.yulore.fc.event.http;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class HttpResponseVO {
    int statusCode;
    String body;
    Boolean isBase64Encoded;
}
