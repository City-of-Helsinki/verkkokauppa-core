package fi.hel.verkkokauppa.productmapping.api.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductMappingDto {

    String productId;
    String namespace;
    String namespaceEntityId;
    String merchantId;
}
