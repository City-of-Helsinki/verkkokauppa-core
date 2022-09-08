package fi.hel.verkkokauppa.order.model;

import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.order.interfaces.Customer;
import fi.hel.verkkokauppa.order.interfaces.IdentifiableUser;
import fi.hel.verkkokauppa.order.model.invoice.Invoice;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Optional;


@Document(indexName = "orders")
@Setter
@Getter
@Slf4j
public class Order implements Customer, IdentifiableUser {
    @Id
    String orderId;

    @Field(type = FieldType.Auto)
    ArrayList<String> subscriptionIds;

    @Field(type = FieldType.Keyword)
    String namespace;

    @Field(type = FieldType.Keyword)
    String user;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    LocalDateTime createdAt;

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
    private LocalDateTime startDate;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private LocalDateTime endDate;

    @Field(type = FieldType.Auto)
    Invoice invoice;

    @Field(type = FieldType.Long)
    Long incrementId;

    public String getFirstSubscriptionId() {
        Optional<String> first = Optional.empty();
        try {
            first = getSubscriptionIds().stream().findFirst();
        } catch (NoSuchElementException exception) {
            log.error("Cant get first subscription id from order subscriptionIds");
        }

        return first.orElseThrow();
    }


    public Order() {}

    public Order(String orderId, String namespace, String user, LocalDateTime createdAt, Long incrementId) {
        this.status = OrderStatus.DRAFT;
        this.type = OrderType.ORDER;

        this.orderId = orderId;
        this.namespace = namespace;
        this.user = user;
        this.createdAt = createdAt;
        this.incrementId = incrementId;
    }
}
