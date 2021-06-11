package fi.hel.verkkokauppa.product.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

@Getter
@Setter
@NoArgsConstructor
public class ProductAccountingDto {

    private String mainLedgerAccount;

    private String vatCode;

    //ToDo: more info needed
    private String internalOrder;

    //ToDo: more info needed
    private String profitCenter;

    //ToDo: more info needed
    private String project;

    //ToDo: more info needed
    private String operationArea;

}
