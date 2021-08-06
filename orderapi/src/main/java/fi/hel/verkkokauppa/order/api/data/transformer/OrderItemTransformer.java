package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import fi.hel.verkkokauppa.order.model.OrderItem;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class OrderItemTransformer implements ITransformer<OrderItem, OrderItemDto> {

    private ModelMapper modelMapper = new ModelMapper();

    @Override
    public OrderItem transformToEntity(OrderItemDto orderItemDto) {
        OrderItem orderItem = modelMapper.map(orderItemDto, OrderItem.class);

        return orderItem;
    }

    @Override
    public OrderItemDto transformToDto(OrderItem orderItem) {
        OrderItemDto orderItemDto = modelMapper.map(orderItem, OrderItemDto.class);
        return orderItemDto;
    }

}
