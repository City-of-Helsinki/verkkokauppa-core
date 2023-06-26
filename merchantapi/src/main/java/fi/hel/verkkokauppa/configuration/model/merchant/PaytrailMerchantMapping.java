package fi.hel.verkkokauppa.configuration.model.merchant;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "paytrail_merchant_mapping")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaytrailMerchantMapping {
    @Id
    String id;

    @Field(type = FieldType.Text)
    String namespace;

    @Field(type = FieldType.Text)
    String merchantPaytrailMerchantId;

    @Field(type = FieldType.Text)
    String merchantPaytrailSecret;

    @Field(type = FieldType.Text)
    String description;
}
