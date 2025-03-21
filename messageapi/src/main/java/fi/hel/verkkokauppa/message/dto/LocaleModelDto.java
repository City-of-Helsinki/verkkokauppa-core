package fi.hel.verkkokauppa.message.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class LocaleModelDto {
    public String fi;
    public String sv;
    public String en;
}
