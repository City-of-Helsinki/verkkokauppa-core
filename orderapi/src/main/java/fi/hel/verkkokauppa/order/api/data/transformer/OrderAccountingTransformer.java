package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.order.api.data.accounting.OrderAccountingDto;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class OrderAccountingTransformer implements ITransformer<OrderAccounting, OrderAccountingDto> {

    private ModelMapper modelMapper = new ModelMapper();

    @Override
    public OrderAccounting transformToEntity(OrderAccountingDto orderAccountingDto) {
        return modelMapper.map(orderAccountingDto, OrderAccounting.class);
    }

    @Override
    public OrderAccountingDto transformToDto(OrderAccounting orderAccounting) {
        return modelMapper.map(orderAccounting, OrderAccountingDto.class);
    }

}
