package fi.hel.verkkokauppa.merchant.model;

import fi.hel.verkkokauppa.common.contracts.merchant.merchant;
import lombok.Builder;
import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Data
@Builder
@Document(indexName = "merchant")
public class MerchantModel implements merchant {
    @Id
    String merchantId;
}
