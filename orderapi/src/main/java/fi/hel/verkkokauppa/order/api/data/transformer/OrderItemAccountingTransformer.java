package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.order.api.data.accounting.OrderItemAccountingDto;
import fi.hel.verkkokauppa.order.model.accounting.OrderItemAccounting;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class OrderItemAccountingTransformer implements ITransformer<OrderItemAccounting, OrderItemAccountingDto> {

    private ModelMapper modelMapper = new ModelMapper();

    @Override
    public OrderItemAccounting transformToEntity(OrderItemAccountingDto orderAccountingDto) {
        return modelMapper.map(orderAccountingDto, OrderItemAccounting.class);
    }

    @Override
    public OrderItemAccountingDto transformToDto(OrderItemAccounting orderAccounting) {
        return modelMapper.map(orderAccounting, OrderItemAccountingDto.class);
    }

}
