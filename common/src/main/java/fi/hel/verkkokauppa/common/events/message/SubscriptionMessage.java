package fi.hel.verkkokauppa.common.events.message;

import fi.hel.verkkokauppa.common.events.message.EventMessage;

public class SubscriptionMessage implements EventMessage {
    public String subscriptionId;
    public String orderId;
    public String namespace;
    public String type;
    public String timestamp;

    public SubscriptionMessage() {
    }

    public SubscriptionMessage(String subscriptionId, String orderId, String namespace, String type, String timestamp) {
        this.subscriptionId = subscriptionId;
        this.orderId = orderId;
        this.namespace = namespace;
        this.type = type;
        this.timestamp = timestamp;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
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
