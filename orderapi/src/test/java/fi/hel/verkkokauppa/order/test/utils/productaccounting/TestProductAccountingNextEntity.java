package fi.hel.verkkokauppa.order.test.utils.productaccounting;

import lombok.Data;

@Data
public class TestProductAccountingNextEntity {
    private String companyCode;

    private String mainLedgerAccount;

    private String vatCode;

    private String internalOrder;

    private String profitCenter;

    private String balanceProfitCenter;

    private String project;

    private String operationArea;
}
