package fi.hel.verkkokauppa.mockproductmanagement.api;

import fi.hel.verkkokauppa.common.response.OrderRightOfPurchaseResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MockOrderRightOfPurchaseController {
    private Logger log = LoggerFactory.getLogger(MockOrderRightOfPurchaseController.class);

    @PostMapping("mock/**/order/right-of-purchase")
    public ResponseEntity<OrderRightOfPurchaseResponse> getOrderRightOfPurchase(@RequestBody String data) {
        try {
            JSONObject orderRightOfPurchaseRequest = new JSONObject(data);
            OrderRightOfPurchaseResponse response = OrderRightOfPurchaseResponse
                    .builder()
                    .orderId(orderRightOfPurchaseRequest.optString("orderId"))
                    .rightOfPurchase(true)
                    .userId(orderRightOfPurchaseRequest.optString("userId"))
                    .orderItemId(orderRightOfPurchaseRequest.optString("orderItemId", "orderItemId"))
                    .errorMessage(orderRightOfPurchaseRequest.optString("errorMessage"))
                    .build();

            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            log.error("getting order right of purchase failed, data: " + data, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
