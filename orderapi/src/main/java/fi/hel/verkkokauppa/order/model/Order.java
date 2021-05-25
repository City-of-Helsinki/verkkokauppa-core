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
    @Field(type = FieldType.Text)
    String status;
    @Field(type = FieldType.Text)
    String type;

    @Field(type = FieldType.Text)
    String customerFirstName;
    @Field(type = FieldType.Text)
    String customerLastName;
    @Field(type = FieldType.Text)
    String customerEmail;

    public Order() {}

    public Order(String orderId, String namespace, String user, String createdAt) {
        this.status = OrderStatus.CREATED;
        this.type = OrderType.ORDER;

        this.orderId = orderId;
        this.namespace = namespace;
        this.user = user;
        this.createdAt = createdAt;
    }
    
    public Order(String orderId, String namespace, String user, String createdAt, String status, String customerFirstName, String customerLastName,
            String customerEmail) {
        this.status = OrderStatus.CREATED;
        this.type = OrderType.ORDER;

        this.orderId = orderId;
        this.namespace = namespace;
        this.user = user;
        this.createdAt = createdAt;
        this.status = status;
        this.customerFirstName = customerFirstName;
        this.customerLastName = customerLastName;
        this.customerEmail = customerEmail;
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

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCustomerFirstName() {
        return customerFirstName;
    }

    public void setCustomerFirstName(String customerFirstName) {
        this.customerFirstName = customerFirstName;
    }

    public String getCustomerLastName() {
        return customerLastName;
    }

    public void setCustomerLastName(String customerLastName) {
        this.customerLastName = customerLastName;
    }
    
}
