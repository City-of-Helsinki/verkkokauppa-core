package fi.hel.verkkokauppa.common.message;

public class OrderMessage {
    public String orderId;
    public String namespace;
    public String type;
    public String timestamp;

    public OrderMessage() {
    }

    public OrderMessage(String orderId, String namespace, String type, String timestamp) {
        this.orderId = orderId;
        this.namespace = namespace;
        this.type = type;
        this.timestamp = timestamp;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
