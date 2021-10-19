package fi.hel.verkkokauppa.common.message;

public class PaymentMessage {
    public String paymentId;
    public String orderId;
    public String namespace;
    public String type;
    public String timestamp;

    public PaymentMessage() {
    }

    public PaymentMessage(String paymentId, String orderId, String namespace, String type, String timestamp) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.namespace = namespace;
        this.type = type;
        this.timestamp = timestamp;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
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
