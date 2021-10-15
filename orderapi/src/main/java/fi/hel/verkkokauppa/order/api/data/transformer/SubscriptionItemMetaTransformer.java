package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.order.api.data.OrderItemMetaDto;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionItemMeta;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionItemMetaTransformer implements ITransformer<SubscriptionItemMeta, OrderItemMetaDto> {

    private ModelMapper modelMapper = new ModelMapper();

    @Override
    public SubscriptionItemMeta transformToEntity(OrderItemMetaDto orderItemDto) {
        return modelMapper.map(orderItemDto, SubscriptionItemMeta.class);
    }

    @Override
    public OrderItemMetaDto transformToDto(SubscriptionItemMeta orderItem) {
        return modelMapper.map(orderItem, OrderItemMetaDto.class);
    }

}