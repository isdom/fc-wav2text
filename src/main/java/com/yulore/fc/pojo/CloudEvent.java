package com.yulore.fc.pojo;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSpecversion() {
        return specversion;
    }

    public void setSpecversion(String specversion) {
        this.specversion = specversion;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDatacontenttype() {
        return datacontenttype;
    }

    public void setDatacontenttype(String datacontenttype) {
        this.datacontenttype = datacontenttype;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getAliyunaccountid() {
        return aliyunaccountid;
    }

    public void setAliyunaccountid(String aliyunaccountid) {
        this.aliyunaccountid = aliyunaccountid;
    }

    public String getAliyunpublishtime() {
        return aliyunpublishtime;
    }

    public void setAliyunpublishtime(String aliyunpublishtime) {
        this.aliyunpublishtime = aliyunpublishtime;
    }

    public String getAliyunoriginalaccountid() {
        return aliyunoriginalaccountid;
    }

    public void setAliyunoriginalaccountid(String aliyunoriginalaccountid) {
        this.aliyunoriginalaccountid = aliyunoriginalaccountid;
    }

    public String getAliyuneventbusname() {
        return aliyuneventbusname;
    }

    public void setAliyuneventbusname(String aliyuneventbusname) {
        this.aliyuneventbusname = aliyuneventbusname;
    }

    public String getAliyunregionid() {
        return aliyunregionid;
    }

    public void setAliyunregionid(String aliyunregionid) {
        this.aliyunregionid = aliyunregionid;
    }

    public String getAliyunpublishaddr() {
        return aliyunpublishaddr;
    }

    public void setAliyunpublishaddr(String aliyunpublishaddr) {
        this.aliyunpublishaddr = aliyunpublishaddr;
    }

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
