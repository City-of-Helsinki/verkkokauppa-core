package fi.hel.verkkokauppa.payment.testing.data;

import lombok.Data;

/**
 * Yritys
 *
 * Pääkirjatili
 *
 * Alv-koodi
 *
 * Sisäinen tilaus
 *
 * Tulosyksikkö
 *
 * Projekti
 *
 * Toimintoalue
 */

@Data
public class ProductAccountingDto {

    private String productId;

    private String companyCode;

    private String mainLedgerAccount;

    private String vatCode;

    private String internalOrder;

    private String profitCenter;

    private String balanceProfitCenter;

    private String project;

    private String operationArea;
}
