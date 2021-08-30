package fi.hel.verkkokauppa.order.api.data.accounting;

import lombok.Data;

/**
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

    private String mainLedgerAccount;

    private String vatCode;

    private String internalOrder;

    private String profitCenter;

    private String project;

    private String operationArea;
}
