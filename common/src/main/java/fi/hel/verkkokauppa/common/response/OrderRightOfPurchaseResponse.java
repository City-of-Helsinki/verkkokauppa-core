package fi.hel.verkkokauppa.common.response;

import fi.hel.verkkokauppa.common.error.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderRightOfPurchaseResponse {
    private String errorMessage;
    private Boolean rightOfPurchase;
    private String orderId;
    private String userId;
    private String orderItemId;
}
