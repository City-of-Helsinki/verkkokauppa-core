package fi.hel.verkkokauppa.order.api.data.accounting;

import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class OrderItemAccountingDto {

    private String orderItemId;

    private String orderId;

    private String priceGross;

    private String priceNet;

    private String mainLedgerAccount;

    private String vatCode;

    private String internalOrder;

    private String profitCenter;

    private String project;

    private String operationArea;

    public OrderItemAccountingDto(String orderItemId, String orderId, String priceGross, String priceNet, ProductAccountingDto productAccountingDto) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.priceGross = priceGross;
        this.priceNet = priceNet;
        this.mainLedgerAccount = productAccountingDto.getMainLedgerAccount();
        this.vatCode = productAccountingDto.getVatCode();
        this.internalOrder = productAccountingDto.getInternalOrder();
        this.profitCenter = productAccountingDto.getProfitCenter();
        this.project = productAccountingDto.getProject();
        this.operationArea = productAccountingDto.getOperationArea();
    }
}
