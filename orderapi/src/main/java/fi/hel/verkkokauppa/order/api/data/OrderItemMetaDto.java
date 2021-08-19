package fi.hel.verkkokauppa.order.api.data;

import lombok.Data;

@Data
public class OrderItemMetaDto {

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

