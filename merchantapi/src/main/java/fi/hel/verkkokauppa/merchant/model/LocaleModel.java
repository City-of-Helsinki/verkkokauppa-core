package fi.hel.verkkokauppa.merchant.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocaleModel {
    @Field(type = FieldType.Text)
    public String fi;
    @Field(type = FieldType.Text)
    public String sv;
    @Field(type = FieldType.Text)
    public String en;
}
