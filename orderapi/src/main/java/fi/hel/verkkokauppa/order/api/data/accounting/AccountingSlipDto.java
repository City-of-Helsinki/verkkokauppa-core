package fi.hel.verkkokauppa.order.api.data.accounting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountingSlipDto {

    private String accountingSlipId;

    private String documentDate;

    private String companyCode;

    private String documentType;

    private String postingDate;

    private String reference;

    private String headerText;

    private String currencyCode;

    private List<AccountingSlipRowDto> rows;

}
