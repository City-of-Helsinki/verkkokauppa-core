package fi.hel.verkkokauppa.order.api.data.accounting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AccountingExportDataDto {

    private String accountingSlipId;

    private String timestamp;

    private String xml;

    private String exported;

    public AccountingExportDataDto(String accountingSlipId, String timestamp, String xml) {
        this.accountingSlipId = accountingSlipId;
        this.timestamp = timestamp;
        this.xml = xml;
    }

}
