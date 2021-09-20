package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.order.api.data.accounting.AccountingExportDataDto;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class AccountingExportDataTransformer implements ITransformer<AccountingExportData, AccountingExportDataDto> {

    private ModelMapper modelMapper = new ModelMapper();

    @Override
    public AccountingExportData transformToEntity(AccountingExportDataDto dto) {
        return modelMapper.map(dto, AccountingExportData.class);
    }

    @Override
    public AccountingExportDataDto transformToDto(AccountingExportData entity) {
        return modelMapper.map(entity, AccountingExportDataDto.class);
    }

}
