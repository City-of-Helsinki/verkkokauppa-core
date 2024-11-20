package fi.hel.verkkokauppa.order.test.utils.productaccounting;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TestProductAccounting {
    private String productId;

    private String companyCode;

    private String mainLedgerAccount;

    private String vatCode;

    private String internalOrder;

    private String profitCenter;

    private String balanceProfitCenter;

    private String project;

    private String operationArea;

    private LocalDateTime activeFrom;

    private TestProductAccountingNextEntity nextEntity;
}
