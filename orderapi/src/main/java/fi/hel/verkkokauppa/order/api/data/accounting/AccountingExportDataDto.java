package fi.hel.verkkokauppa.order.api.data.accounting;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class AccountingExportDataDto {

    private String accountingSlipId;

    private LocalDateTime timestamp;

    private String xml;

    private String exported;

    public AccountingExportDataDto(String accountingSlipId, LocalDateTime timestamp, String xml) {
        this.accountingSlipId = accountingSlipId;
        this.timestamp = timestamp;
        this.xml = xml;
    }

}
