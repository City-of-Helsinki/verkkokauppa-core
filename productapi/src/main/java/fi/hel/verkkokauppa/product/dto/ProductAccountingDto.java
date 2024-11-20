package fi.hel.verkkokauppa.product.dto;

import fi.hel.verkkokauppa.product.model.NextEntity;
import lombok.Data;

import java.time.LocalDateTime;

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
public class ProductAccountingDto extends BaseDto {

    private String productId;

    private String companyCode;

    private String mainLedgerAccount;

    private String vatCode;

    private String internalOrder;

    private String profitCenter;

    private String balanceProfitCenter;

    private String project;

    private String operationArea;

    private String namespace;

//    /**
//     *
//     */
//    private String activeFrom;
    private LocalDateTime activeFrom;

    private NextEntity nextEntity;
}
