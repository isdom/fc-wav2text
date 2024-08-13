package com.yulore.fc.rbt2text;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class Test {
    public static void main(String[] args) throws IOException {
        final String json = "[{"
            + "\"id\": \"bj694332-4cj1-389e-9d8c-b137h30b****\","
            + "\"source\": \"RabbitMQ-Function-rabbitmq-trigger\","
            + "\"specversion\": \"1.0\","
            + "\"type\": \"amqp:Queue:SendMessage\","
            + "\"datacontenttype\": \"application/json;charset=utf-8\","
            +"\"subject\": \"acs:amqp:cn-hangzhou:164901546557****:/instances/amqp-cn-tl32e756****/vhosts/eb-connect/queues/housekeeping\","
            +"\"data\": {"
            +"\"envelope\": {"
            +" \"deliveryTag\": 98,"
            +"           \"exchange\": \"\","
            +"           \"redeliver\": false,"
            +"           \"routingKey\": \"housekeeping\""
            +"},"
            +"\"body\": {"
            +"   \"sessionId\": \"sessionId-test\""
            +"},"
            +"\"props\": {"
            +"   \"contentEncoding\": \"UTF-8\","
            +"           \"messageId\": \"f7622d51-e198-41de-a072-77c1ead7****\""
            +"}"
        +"}}]";
        System.out.println(json);
        RbtEvent[] events = new ObjectMapper().readValue(json, RbtEvent[].class);
        for (RbtEvent event : events) {
            System.out.println(event);
        }
    }
}
