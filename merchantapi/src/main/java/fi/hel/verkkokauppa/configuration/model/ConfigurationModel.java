package fi.hel.verkkokauppa.configuration.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
