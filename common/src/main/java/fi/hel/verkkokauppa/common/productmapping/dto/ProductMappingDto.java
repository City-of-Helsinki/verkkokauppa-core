package fi.hel.verkkokauppa.common.productmapping.dto;

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
