package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.order.api.data.OrderItemMetaDto;
import fi.hel.verkkokauppa.order.model.OrderItemMeta;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class OrderItemMetaTransformer implements ITransformer<OrderItemMeta, OrderItemMetaDto> {

    private ModelMapper modelMapper = new ModelMapper();

    @Override
    public OrderItemMeta transformToEntity(OrderItemMetaDto orderItemDto) {
        OrderItemMeta orderItem = modelMapper.map(orderItemDto, OrderItemMeta.class);

        return orderItem;
    }

    @Override
    public OrderItemMetaDto transformToDto(OrderItemMeta orderItem) {
        OrderItemMetaDto orderItemDto = modelMapper.map(orderItem, OrderItemMetaDto.class);
        return orderItemDto;
    }

}