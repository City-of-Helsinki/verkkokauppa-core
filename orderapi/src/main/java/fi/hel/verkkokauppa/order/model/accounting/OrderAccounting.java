package fi.hel.verkkokauppa.order.model.accounting;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "orderaccountings")
@Data
public class OrderAccounting {

    @Id
    private String orderId;

    @Field(type = FieldType.Text)
    private String createdAt;

}
