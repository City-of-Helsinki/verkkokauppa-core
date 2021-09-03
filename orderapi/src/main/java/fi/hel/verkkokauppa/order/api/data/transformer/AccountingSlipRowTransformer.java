package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.order.api.data.accounting.AccountingSlipRowDto;
import fi.hel.verkkokauppa.order.model.accounting.AccountingSlipRow;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class AccountingSlipRowTransformer implements ITransformer<AccountingSlipRow, AccountingSlipRowDto> {

    private ModelMapper modelMapper = new ModelMapper();

    @Override
    public AccountingSlipRow transformToEntity(AccountingSlipRowDto orderAccountingDto) {
        return modelMapper.map(orderAccountingDto, AccountingSlipRow.class);
    }

    @Override
    public AccountingSlipRowDto transformToDto(AccountingSlipRow orderAccounting) {
        return modelMapper.map(orderAccounting, AccountingSlipRowDto.class);
    }

}
