package fi.hel.verkkokauppa.order.api.request.rightOfPurchase;

import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderRightOfPurchaseRequest {
    private String orderId;
    private String namespace;
    private String userId;
    private OrderItemDto orderItem;

    public static OrderRightOfPurchaseRequest fromOrderItemDto(OrderItemDto orderItem, String userId, String namespace) {
        return OrderRightOfPurchaseRequest.builder()
                .orderId(orderItem.getOrderId())
                .userId(userId)
                .orderItem(orderItem)
                .namespace(namespace)
                .build();
    }
}
