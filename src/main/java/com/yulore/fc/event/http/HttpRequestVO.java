package com.yulore.fc.event.http;

import lombok.Data;
import lombok.ToString;

import java.util.Map;

@Data
@ToString
public class HttpRequestVO {
    // REF: https://help.aliyun.com/zh/functioncompute/fc-3-0/user-guide/http-trigger-invoking-function
    String version;
    String rawPath;
    String body;
    Boolean isBase64Encoded;
    Map<String, String> headers;
    Map<String, String> queryParameters;

    /*
    "requestContext": {
        "accountId": "123456*********",
        "domainName": "<http-trigger-id>.<region-id>.fcapp.run",
        "domainPrefix": "<http-trigger-id>",
        "http": {
            "method": "GET",
            "path": "/example",
            "protocol": "HTTP/1.1",
            "sourceIp": "11.11.11.**",
            "userAgent": "PostmanRuntime/7.32.3"
        },
        "requestId": "1-64f6cd87-*************",
        "time": "2023-09-05T06:41:11Z",
        "timeEpoch": "1693896071895"
    }
    */

    @Data
    @ToString
    public static class Http {
        String method;
        String path;
        String protocol;
        String sourceIp;
        String userAgent;
    }

    @Data
    @ToString
    public static class RequestContext {
        String accountId;
        String domainName;
        String domainPrefix;
        String requestId;
        String time;
        String timeEpoch;
        Http http;
    }
    RequestContext requestContext;
}
