package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.order.api.data.OrderDto;
import fi.hel.verkkokauppa.order.model.Order;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class OrderTransformer implements ITransformer<Order, OrderDto> {

    private ModelMapper modelMapper = new ModelMapper();

    @Override
    public Order transformToEntity(OrderDto orderDto) {
        Order order = modelMapper.map(orderDto, Order.class);
        return order;
    }

    @Override
    public OrderDto transformToDto(Order order) {
        OrderDto orderDto = modelMapper.map(order, OrderDto.class);
        return orderDto;
    }
}
