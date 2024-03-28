package fi.hel.verkkokauppa.message.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemMetaDto {
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
