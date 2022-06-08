package fi.hel.verkkokauppa.merchant.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Data
@Builder
@Document(indexName = "merchant")
public class MerchantModel {
    @Id
    String merchantId;
}
