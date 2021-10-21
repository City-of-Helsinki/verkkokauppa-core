package fi.hel.verkkokauppa.order.api.data.accounting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountingExportDataDto {

    private String accountingSlipId;

    private String timestamp;

    private String xml;

}
