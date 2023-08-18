package fi.hel.verkkokauppa.order.model;

import fi.hel.verkkokauppa.common.contracts.OrderItemSubscriptionFields;
import fi.hel.verkkokauppa.order.interfaces.Product;
import fi.hel.verkkokauppa.order.logic.OrderTypeLogic;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(indexName = "orderitems")
@Getter
@Setter
public class OrderItem implements OrderItemSubscriptionFields, Product {

    @Id
    String orderItemId;
    @Field(type = FieldType.Keyword)
    String orderId;
    @Field(type = FieldType.Keyword)
    String merchantId;

    @Field(type = FieldType.Keyword)
    String productId;
    @Field(type = FieldType.Text)
    String productName;
    @Field(type = FieldType.Text)
    String productLabel;
    @Field(type = FieldType.Text)
    String productDescription;
    @Field(type = FieldType.Text)
    Integer quantity;
    @Field(type = FieldType.Text)
    String unit;

    @Field(type = FieldType.Text)
    String rowPriceNet;
    @Field(type = FieldType.Text)
    String rowPriceVat;
    @Field(type = FieldType.Text)
    String rowPriceTotal;

    @Field(type = FieldType.Text)
    String vatPercentage;
    @Field(type = FieldType.Text)
    String priceNet;
    @Field(type = FieldType.Text)
    String priceVat;
    @Field(type = FieldType.Text)
    String priceGross;
    @Field(type = FieldType.Text)
    String originalPriceNet;
    @Field(type = FieldType.Text)
    String originalPriceVat;
    @Field(type = FieldType.Text)
    String originalPriceGross;
    @Field(type = FieldType.Text)
    String type;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private LocalDateTime startDate; // TODO: Test for date_optional

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private LocalDateTime billingStartDate; // TODO: Test for date_optional

    // Subscription fields
    @Field(type = FieldType.Text)
    private String periodUnit;
    @Field(type = FieldType.Long)
    private Long periodFrequency;
    @Field(type = FieldType.Long)
    private Integer periodCount;

    @Field(type = FieldType.Date, format = DateFormat.date)
    LocalDate invoicingDate;

    @Field(type = FieldType.Text)
    String invoicingStatus;

    public OrderItem() {}

    public OrderItem(
            String orderItemId,
            String orderId,
            String merchantId,
            String productId,
            String productName,
            String productLabel,
            String productDescription,
            Integer quantity,
            String unit,
            String rowPriceNet,
            String rowPriceVat,
            String rowPriceTotal,
            String vatPercentage,
            String priceNet,
            String priceVat,
            String priceGross,
            String originalPriceNet,
            String originalPriceVat,
            String originalPriceGross,
            String periodUnit,
            Long periodFrequency,
            Integer periodCount,
            LocalDateTime billingStartDate,
            LocalDateTime startDate,
            LocalDate invoicingDate
    ) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.merchantId = merchantId;
        this.productId = productId;
        this.productName = productName;
        this.productLabel = productLabel;
        this.productDescription = productDescription;
        this.quantity = quantity;
        this.unit = unit;
        this.rowPriceNet = rowPriceNet;
        this.rowPriceVat = rowPriceVat;
        this.rowPriceTotal = rowPriceTotal;
        this.vatPercentage = vatPercentage;
        this.priceNet = priceNet;
        this.priceVat = priceVat;
        this.priceGross = priceGross;
        this.originalPriceNet = originalPriceNet;
        this.originalPriceVat = originalPriceVat;
        this.originalPriceGross = originalPriceGross;
        // Subscription fields
        this.periodUnit = periodUnit;
        this.periodFrequency = periodFrequency;
        this.periodCount = periodCount;
        this.billingStartDate = billingStartDate;
        this.startDate = startDate;
        this.type = OrderTypeLogic.isSubscription(this) ? OrderItemType.SUBSCRIPTION : OrderItemType.SINGLE;
        this.invoicingDate = invoicingDate;
    }

}
