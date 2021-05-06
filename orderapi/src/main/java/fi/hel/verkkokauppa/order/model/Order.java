package fi.hel.verkkokauppa.order.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "orders")
public class Order {
    @Id
    String orderId;
    @Field(type = FieldType.Keyword)
    String namespace;
    @Field(type = FieldType.Keyword)
    String user;
    @Field(type = FieldType.Text)
    String createdAt;

    public Order() {}

    public Order(String orderId, String namespace, String user, String createdAt) {
        this.orderId = orderId;
        this.namespace = namespace;
        this.user = user;
        this.createdAt = createdAt;
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

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

}
