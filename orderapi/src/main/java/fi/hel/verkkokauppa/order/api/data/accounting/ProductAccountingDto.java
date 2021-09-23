package fi.hel.verkkokauppa.order.api.data.accounting;

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

    private String project;

    private String operationArea;
}
