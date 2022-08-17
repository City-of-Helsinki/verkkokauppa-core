package fi.hel.verkkokauppa.order.model.accounting;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "orderaccountings")
@Data
public class OrderAccounting {

    @Id
    private String orderId;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private LocalDateTime createdAt;

}
