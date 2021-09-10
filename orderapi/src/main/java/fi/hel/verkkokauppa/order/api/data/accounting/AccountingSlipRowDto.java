package fi.hel.verkkokauppa.order.api.data.accounting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountingSlipRowDto {

    private String accountingSlipRowId;

    private String accountingSlipId;

    private String amountInDocumentCurrency;

    private String baseAmount;

    private String lineText;

    private String GLAccount;

    private String taxCode;

    private String orderItemNumber;

    private String profitCenter;

    private String WBS_Element;

    private String functionalArea;

}
