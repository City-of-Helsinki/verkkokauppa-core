package fi.hel.verkkokauppa.order.model.accounting;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Document(indexName = "orderaccountings")
@Data
public class OrderAccounting {
    public static final String INDEX_NAME = "orderaccountings";

    @Id
    private String orderId;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private Instant createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date)
    LocalDate accounted;

    @Field(type = FieldType.Text)
    private String namespace;

    public LocalDateTime getCreatedAt(){
        return LocalDateTime.ofInstant(this.createdAt, ZoneOffset.UTC);
    }

    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt.atZone(ZoneOffset.UTC).toInstant();
    }
}
