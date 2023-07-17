package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.order.api.data.accounting.RefundItemAccountingDto;
import fi.hel.verkkokauppa.order.model.accounting.RefundItemAccounting;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class RefundItemAccountingTransformer implements ITransformer<RefundItemAccounting, RefundItemAccountingDto> {

    private ModelMapper modelMapper = new ModelMapper();

    @Override
    public RefundItemAccounting transformToEntity(RefundItemAccountingDto refundAccountingDto) {
        return modelMapper.map(refundAccountingDto, RefundItemAccounting.class);
    }

    @Override
    public RefundItemAccountingDto transformToDto(RefundItemAccounting refundAccounting) {
        return modelMapper.map(refundAccounting, RefundItemAccountingDto.class);
    }

}
