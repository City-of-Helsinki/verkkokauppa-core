package fi.hel.verkkokauppa.order.model.invoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Invoice {
    @Field(type = FieldType.Keyword)
    String businessId;
    @Field(type = FieldType.Keyword)
    String name;
    @Field(type = FieldType.Keyword)
    String address;
    @Field(type = FieldType.Keyword)
    String postcode;
    @Field(type = FieldType.Keyword)
    String city;
    @Field(type = FieldType.Keyword)
    String ovtId;
}
