package fi.hel.verkkokauppa.mockproductmanagement.api.subscription.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MockOrderItemMetaDto {

    // generated or enriched
    private String orderItemMetaId;
    private String orderItemId;
    private String orderId;
    
    // expected as input
    private String key;
    private String value;
    private String label;
    private String visibleInCheckout;
    private String ordinal;

}

