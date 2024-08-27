package com.yulore.fc.pojo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class CloudEvent {
    String id;
    String source;
    String specversion;
    String type;
    String datacontenttype;
    String subject;
    String time;
    String aliyunaccountid;
    String aliyunpublishtime;
    String aliyunoriginalaccountid;
    String aliyuneventbusname;
    String aliyunregionid;
    String aliyunpublishaddr;
}
