package fi.hel.verkkokauppa.payment.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "payment_filter")
@Data
public class PaymentFilter {
    @Id
    String filterId;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    LocalDateTime updatedAt;

    @Field(type = FieldType.Text)
    String namespace;

    @Field(type = FieldType.Text)
    String referenceId;

    @Field(type = FieldType.Text)
    String type;

    @Field(type = FieldType.Text)
    String value;
}
