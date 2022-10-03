package fi.hel.verkkokauppa.productmapping.response.namespace;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigurationModel {
    @Field(type = FieldType.Text)
    public String key;
    @Field(type = FieldType.Text)
    public String value;
}
