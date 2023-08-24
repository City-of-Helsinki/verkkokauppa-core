package fi.hel.verkkokauppa.order.model.invoice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(indexName = "orderiteminvoicings")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemInvoicing {
    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    LocalDateTime updatedAt;

    @Field(type = FieldType.Text)
    OrderItemInvoicingStatus status;

    @Id
    String orderItemId;

    @Field(type = FieldType.Text)
    String orderId;

    @Field(type = FieldType.Text)
    String orderIncrementId;

    @Field(type = FieldType.Date, format = DateFormat.date)
    LocalDate invoicingDate;

    @Field(type = FieldType.Text)
    String customerYid;

    @Field(type = FieldType.Text)
    String customerOvt;

    @Field(type = FieldType.Text)
    String material;

    @Field(type = FieldType.Text)
    String materialDescription;

    @Field(type = FieldType.Text)
    Integer quantity;

    @Field(type = FieldType.Text)
    String unit;

    @Field(type = FieldType.Text)
    String priceNet;
}
