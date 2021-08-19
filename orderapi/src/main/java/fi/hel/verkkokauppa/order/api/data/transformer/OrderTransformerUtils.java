package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemMetaDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.OrderItemMeta;

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

    @Autowired
    private OrderItemMetaTransformer orderItemMetaTransformer;


    public OrderAggregateDto transformToOrderAggregateDto(Order order, List<OrderItem> orderItems, List<OrderItemMeta> metas) {
        OrderAggregateDto dto = new OrderAggregateDto();
        dto.setOrder(orderTransformer.transformToDto(order));

        if (!orderItems.isEmpty()) {
            List<OrderItemDto> orderItemDtoList = orderItems.stream()
                .map(orderItem -> orderItemTransformer.transformToDto(orderItem))
                .collect(Collectors.toList());

            dto.setItems(orderItemDtoList);
        }

        if (!metas.isEmpty()) {
            dto.getItems().stream().forEach(item -> {
                List<OrderItemMeta> itemMetas = metas.stream()
                    .filter(meta -> meta.getOrderItemId().equals(item.getOrderItemId()))
                    .collect(Collectors.toList());

                List<OrderItemMetaDto> itemMetaDtos = itemMetas.stream()
                    .map(itemMeta -> orderItemMetaTransformer.transformToDto(itemMeta))
                    .collect(Collectors.toList());
                
                item.setMeta(itemMetaDtos);
            });
        }

        return dto;
    }

}
