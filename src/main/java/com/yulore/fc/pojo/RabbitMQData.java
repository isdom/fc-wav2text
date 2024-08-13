package com.yulore.fc.pojo;

public class RabbitMQData {
    static class Props {
        String contentEncoding;
        String messageId;

        public String getContentEncoding() {
            return contentEncoding;
        }

        public void setContentEncoding(String contentEncoding) {
            this.contentEncoding = contentEncoding;
        }

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Props{");
            sb.append("contentEncoding='").append(contentEncoding).append('\'');
            sb.append(", messageId='").append(messageId).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
    static class Envelope {
        Integer deliveryTag;
        String exchange;
        Boolean redeliver;
        String routingKey;

        public Integer getDeliveryTag() {
            return deliveryTag;
        }

        public void setDeliveryTag(Integer deliveryTag) {
            this.deliveryTag = deliveryTag;
        }

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public Boolean getRedeliver() {
            return redeliver;
        }

        public void setRedeliver(Boolean redeliver) {
            this.redeliver = redeliver;
        }

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Envelope{");
            sb.append("deliveryTag=").append(deliveryTag);
            sb.append(", exchange='").append(exchange).append('\'');
            sb.append(", redeliver=").append(redeliver);
            sb.append(", routingKey='").append(routingKey).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    Props props;
    Envelope envelope;

    public Props getProps() {
        return props;
    }

    public void setProps(Props props) {
        this.props = props;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RabbitMQData{");
        sb.append("props=").append(props);
        sb.append(", envelope=").append(envelope);
        sb.append('}');
        return sb.toString();
    }
}
