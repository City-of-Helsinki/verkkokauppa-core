package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.order.api.data.accounting.AccountingSlipDto;
import fi.hel.verkkokauppa.order.model.accounting.AccountingSlip;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class AccountingSlipTransformer implements ITransformer<AccountingSlip, AccountingSlipDto> {

    private ModelMapper modelMapper = new ModelMapper();

    @Override
    public AccountingSlip transformToEntity(AccountingSlipDto orderAccountingDto) {
        return modelMapper.map(orderAccountingDto, AccountingSlip.class);
    }

    @Override
    public AccountingSlipDto transformToDto(AccountingSlip orderAccounting) {
        return modelMapper.map(orderAccounting, AccountingSlipDto.class);
    }

}
