package fi.hel.verkkokauppa.order.model;

import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.order.interfaces.Customer;
import fi.hel.verkkokauppa.order.interfaces.IdentifiableUser;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;


@Document(indexName = "orders")
public class Order implements Customer, IdentifiableUser {
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
    @Field(type = FieldType.Text)
    String customerPhone;

    @Field(type = FieldType.Text)
    String priceNet;
    @Field(type = FieldType.Text)
    String priceVat;
    @Field(type = FieldType.Text)
    String priceTotal;

    @Field(type = FieldType.Date, format = DateFormat.date)
    String accounted;

    public Order() {}

    public Order(String orderId, String namespace, String user, String createdAt) {
        this.status = OrderStatus.DRAFT;
        this.type = OrderType.ORDER;

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

    public String getPriceNet() {
        return priceNet;
    }

    public void setPriceNet(String priceNet) {
        this.priceNet = priceNet;
    }

    public String getPriceVat() {
        return priceVat;
    }

    public void setPriceVat(String priceVat) {
        this.priceVat = priceVat;
    }

    public String getPriceTotal() {
        return priceTotal;
    }

    public void setPriceTotal(String priceTotal) {
        this.priceTotal = priceTotal;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getAccounted() {
        return accounted;
    }

    public void setAccounted(String accounted) {
        this.accounted = accounted;
    }
}
