package fi.hel.verkkokauppa.payment.model.paytrail.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormFieldModel {
    @Field(type = FieldType.Keyword)
    private String name;

    @Field(type = FieldType.Keyword)
    private String value;
}
