package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.order.api.data.accounting.RefundAccountingDto;
import fi.hel.verkkokauppa.order.model.accounting.RefundAccounting;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class RefundAccountingTransformer implements ITransformer<RefundAccounting, RefundAccountingDto> {

    private ModelMapper modelMapper = new ModelMapper();

    @Override
    public RefundAccounting transformToEntity(RefundAccountingDto refundAccountingDto) {
        return modelMapper.map(refundAccountingDto, RefundAccounting.class);
    }

    @Override
    public RefundAccountingDto transformToDto(RefundAccounting refundAccounting) {
        return modelMapper.map(refundAccounting, RefundAccountingDto.class);
    }

}
