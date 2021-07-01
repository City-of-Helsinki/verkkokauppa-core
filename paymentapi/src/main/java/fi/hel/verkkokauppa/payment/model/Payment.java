package fi.hel.verkkokauppa.payment.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "payments")
public class Payment {
    @Id
    String paymentId;
    @Field(type = FieldType.Keyword)
    String namespace;
    @Field(type = FieldType.Text)
    String orderId;

    @Field(type = FieldType.Text)
    String paymentType;
    @Field(type = FieldType.Text)
    String sum;
    @Field(type = FieldType.Text)
    String description;
    @Field(type = FieldType.Text)
    String status;

    public Payment() {}

    public Payment(String paymentId, String namespace, String orderId, String paymentType, String sum,
            String description) {
        this.paymentId = paymentId;
        this.namespace = namespace;
        this.orderId = orderId;
        this.paymentType = paymentType;
        this.sum = sum;
        this.description = description;

        this.status = PaymentStatus.CREATED;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getSum() {
        return sum;
    }

    public void setSum(String sum) {
        this.sum = sum;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
