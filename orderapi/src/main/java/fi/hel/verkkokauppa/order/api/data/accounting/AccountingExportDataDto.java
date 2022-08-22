package fi.hel.verkkokauppa.order.api.data.accounting;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class AccountingExportDataDto {

    private String accountingSlipId;

    private LocalDate timestamp;

    private String xml;

    private LocalDate exported;

    public AccountingExportDataDto(String accountingSlipId, LocalDate timestamp, String xml) {
        this.accountingSlipId = accountingSlipId;
        this.timestamp = timestamp;
        this.xml = xml;
    }

}
