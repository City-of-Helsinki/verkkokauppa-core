package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderTransformerUtils {

    @Autowired
    private OrderTransformer orderTransformer;

    @Autowired
    private OrderItemTransformer orderItemTransformer;

    public OrderAggregateDto transformToOrderAggregateDto(Order order, List<OrderItem> orderItems) {
        OrderAggregateDto dto = new OrderAggregateDto();
        dto.setOrder(orderTransformer.transformToDto(order));
        List<OrderItemDto> orderItemDtoList = orderItems.stream()
                .map(orderItem -> orderItemTransformer.transformToDto(orderItem))
                .collect(Collectors.toList());

        dto.setItems(orderItemDtoList);

        return dto;
    }

}
