package fi.hel.verkkokauppa.order.api.data.accounting;

import lombok.Data;
import org.joda.time.DateTime;

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
public class NextEntityDto {

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
