package fi.hel.verkkokauppa.order.model.subscription;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "subscription_item_metas")
public class SubscriptionItemMeta {

    @Id
    String orderItemMetaId;
    @Field(type = FieldType.Keyword)
    String orderItemId;
    @Field(type = FieldType.Keyword)
    String orderId;
    @Field(type = FieldType.Keyword)
    String subscriptionId;

    @Field(type = FieldType.Keyword)
    String key;
    @Field(type = FieldType.Text)
    String value;
    @Field(type = FieldType.Text)
    String label;
    @Field(type = FieldType.Text)
    String visibleInCheckout;
    @Field(type = FieldType.Text)
    String ordinal;

    public SubscriptionItemMeta() {
    }

    public SubscriptionItemMeta(String orderItemMetaId, String orderItemId, String orderId, String subscriptionId, String key, String value,
                                String label, String visibleInCheckout, String ordinal) {
        this.orderItemMetaId = orderItemMetaId;
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.key = key;
        this.value = value;
        this.label = label;
        this.visibleInCheckout = visibleInCheckout;
        this.ordinal = ordinal;
    }

    public String getOrderItemMetaId() {
        return orderItemMetaId;
    }

    public void setOrderItemMetaId(String orderItemMetaId) {
        this.orderItemMetaId = orderItemMetaId;
    }

    public String getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(String orderItemId) {
        this.orderItemId = orderItemId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getVisibleInCheckout() {
        return visibleInCheckout;
    }

    public void setVisibleInCheckout(String visibleInCheckout) {
        this.visibleInCheckout = visibleInCheckout;
    }

    public String getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(String ordinal) {
        this.ordinal = ordinal;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }
}
