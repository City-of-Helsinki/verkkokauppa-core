package fi.hel.verkkokauppa.order.model;

import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.order.interfaces.Customer;
import fi.hel.verkkokauppa.order.interfaces.IdentifiableUser;
import fi.hel.verkkokauppa.order.model.invoice.Invoice;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


@Document(indexName = "orders")
@Setter
@Getter
public class Order implements Customer, IdentifiableUser {

    public static final String INDEX_NAME = "orders";

    @Id
    String orderId;

    @Field(type = FieldType.Text)
    String subscriptionId;

    @Field(type = FieldType.Keyword)
    String namespace;

    @Field(type = FieldType.Keyword)
    String user;

    @Field(type = FieldType.Date, format = {DateFormat.date_optional_time})
    Instant createdAt;

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
    LocalDate accounted;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private Instant startDate;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private Instant endDate;

    @Field(type = FieldType.Auto)
    Invoice invoice;

    @Field(type = FieldType.Long)
    Long incrementId;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private Instant lastValidPurchaseDateTime;

    public Order() {}

    public Order(String orderId, String namespace, String user, LocalDateTime createdAt, Long incrementId) {
        this.status = OrderStatus.DRAFT;
        this.type = OrderType.ORDER;

        this.orderId = orderId;
        this.namespace = namespace;
        this.user = user;
        this.createdAt = createdAt.atZone(ZoneOffset.UTC).toInstant();
        this.incrementId = incrementId;
    }

    public LocalDateTime getCreatedAt(){
        return LocalDateTime.ofInstant(this.createdAt, ZoneOffset.UTC);
    }

    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt.atZone(ZoneOffset.UTC).toInstant();
    }

    public LocalDateTime getLastValidPurchaseDateTime(){
        if(this.lastValidPurchaseDateTime != null) {
            return LocalDateTime.ofInstant(this.lastValidPurchaseDateTime, ZoneOffset.UTC);
        }
        else{
            return null;
        }
    }

    public void setLastValidPurchaseDateTime(LocalDateTime lastValidPurchaseDateTime){
        this.lastValidPurchaseDateTime = lastValidPurchaseDateTime.atZone(ZoneOffset.UTC).toInstant();
    }

    public LocalDateTime getStartDate() {
        return LocalDateTime.ofInstant(this.startDate, ZoneOffset.UTC);
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate.atZone(ZoneOffset.UTC).toInstant();;
    }

    public LocalDateTime getEndDate() {
        return LocalDateTime.ofInstant(this.endDate, ZoneOffset.UTC);
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate.atZone(ZoneOffset.UTC).toInstant();;
    }

}
