package fi.hel.verkkokauppa.merchant.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Data
@Document(indexName = "merchant")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MerchantModel {
    @Id
    String merchantId;

    @Field(type = FieldType.Text)
    String namespace;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    LocalDateTime updatedAt;

    @Field(type = FieldType.Auto)
    ArrayList<ConfigurationModel> configurations;
}

