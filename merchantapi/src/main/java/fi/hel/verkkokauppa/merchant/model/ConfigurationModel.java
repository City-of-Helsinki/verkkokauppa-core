package fi.hel.verkkokauppa.merchant.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigurationModel {
    @Field(type = FieldType.Text)
    public String key;
    @Field(type = FieldType.Text)
    public String value;
    @Field(type = FieldType.Boolean)
    public boolean restricted;
    @Field(type = FieldType.Auto)
    LocaleModel locale;
}
