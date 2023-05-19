package fi.hel.verkkokauppa.order.api.data.accounting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Yritys
 * <p>
 * Pääkirjatili
 * <p>
 * Alv-koodi
 * <p>
 * Sisäinen tilaus
 * <p>
 * Tulosyksikkö
 * <p>
 * Projekti
 * <p>
 * Toimintoalue
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefundItemAccountingDto {

    private String refundItemId;

    private String refundId;

    private String orderId;

    private String priceGross;

    private String priceNet;

    private String priceVat;

    private String companyCode;

    private String mainLedgerAccount;

    private String vatCode;

    private String internalOrder;

    private String profitCenter;

    private String balanceProfitCenter;

    private String project;

    private String operationArea;

    public RefundItemAccountingDto(String refundItemId, String refundId, String orderId, String priceGross, String priceNet, String priceVat, ProductAccountingDto productAccountingDto) {
        this.refundItemId = refundItemId;
        this.refundId = refundId;
        this.orderId = orderId;
        this.priceGross = priceGross;
        this.priceNet = priceNet;
        this.priceVat = priceVat;
        this.companyCode = productAccountingDto.getCompanyCode();
        this.mainLedgerAccount = productAccountingDto.getMainLedgerAccount();
        this.vatCode = productAccountingDto.getVatCode();
        this.internalOrder = productAccountingDto.getInternalOrder();
        this.profitCenter = productAccountingDto.getProfitCenter();
        this.balanceProfitCenter = productAccountingDto.getBalanceProfitCenter();
        this.project = productAccountingDto.getProject();
        this.operationArea = productAccountingDto.getOperationArea();
    }

    public RefundItemAccountingDto(String mainLedgerAccount, String vatCode, String internalOrder, String profitCenter, String project, String operationArea, String balanceProfitCenter) {
        this.mainLedgerAccount = mainLedgerAccount;
        this.vatCode = vatCode;
        this.internalOrder = internalOrder;
        this.profitCenter = profitCenter;
        this.project = project;
        this.operationArea = operationArea;
        this.balanceProfitCenter = balanceProfitCenter;
    }

    public RefundItemAccountingDto createKey() {
        return new RefundItemAccountingDto(mainLedgerAccount, vatCode, internalOrder, profitCenter, project, operationArea, balanceProfitCenter);
    }

    public String getPriceGross() {
        return priceGross;
    }

    @JsonIgnore
    public Double getPriceGrossAsDouble() {
        return Double.valueOf(priceGross);
    }

    public void setPriceGross(String priceGross) {
        this.priceGross = priceGross;
    }

    public void setPriceGross(Double priceGross) {
        this.priceGross = Double.toString(priceGross);
    }

    public String getPriceNet() {
        return priceNet;
    }

    @JsonIgnore
    public Double getPriceNetAsDouble() {
        return Double.valueOf(priceNet);
    }

    public void setPriceNet(String priceNet) {
        this.priceNet = priceNet;
    }

    public void setPriceNet(Double priceNet) {
        this.priceNet = Double.toString(priceNet);
    }

    public String getPriceVat() {
        return priceVat;
    }

    @JsonIgnore
    public Double getPriceVatAsDouble() {
        return Double.valueOf(priceVat);
    }

    public void setPriceVat(String priceVat) {
        this.priceVat = priceVat;
    }

    public void setPriceVat(Double priceVat) {
        this.priceVat = Double.toString(priceVat);
    }

    public RefundItemAccountingDto merge(RefundItemAccountingDto other) {
        if (other.equals(this)) {
            this.sumPrices(other);
        }

        return this;
    }

    public void sumPrices(RefundItemAccountingDto other) {
        setPriceGross(Double.sum(this.getPriceGrossAsDouble(), other.getPriceGrossAsDouble()));
        setPriceNet(Double.sum(this.getPriceNetAsDouble(), other.getPriceNetAsDouble()));
        setPriceVat(Double.sum(this.getPriceVatAsDouble(), other.getPriceVatAsDouble()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefundItemAccountingDto that = (RefundItemAccountingDto) o;
        return Objects.equals(mainLedgerAccount, that.mainLedgerAccount) && Objects.equals(vatCode, that.vatCode) && Objects.equals(internalOrder, that.internalOrder) && Objects.equals(profitCenter, that.profitCenter) && Objects.equals(project, that.project) && Objects.equals(operationArea, that.operationArea);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mainLedgerAccount, vatCode, internalOrder, profitCenter, project, operationArea);
    }

}
