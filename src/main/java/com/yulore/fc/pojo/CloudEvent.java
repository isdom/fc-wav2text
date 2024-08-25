package com.yulore.fc.pojo;

import lombok.Data;

@Data
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudEvent{");
        sb.append("id='").append(id).append('\'');
        sb.append(", source='").append(source).append('\'');
        sb.append(", specversion='").append(specversion).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", datacontenttype='").append(datacontenttype).append('\'');
        sb.append(", subject='").append(subject).append('\'');
        sb.append(", time='").append(time).append('\'');
        sb.append(", aliyunaccountid='").append(aliyunaccountid).append('\'');
        sb.append(", aliyunpublishtime='").append(aliyunpublishtime).append('\'');
        sb.append(", aliyunoriginalaccountid='").append(aliyunoriginalaccountid).append('\'');
        sb.append(", aliyuneventbusname='").append(aliyuneventbusname).append('\'');
        sb.append(", aliyunregionid='").append(aliyunregionid).append('\'');
        sb.append(", aliyunpublishaddr='").append(aliyunpublishaddr).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
