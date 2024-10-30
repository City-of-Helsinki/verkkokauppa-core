package fi.hel.verkkokauppa.order.api.data.accounting;

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
    private LocalDateTime activeFrom;
    private NextAccountingEntityDto nextEntity;
    private LocalDateTime paidAt;
    private LocalDateTime refundCreatedAt;
    private String merchantId;
    private String namespace;
    private String paytrailTransactionId;
    private String refundTransactionId;

    public String getCompanyCode() {
        if (isActiveFromExceeded() && nextEntity != null) {
            return nextEntity.getCompanyCode() != null ? nextEntity.getCompanyCode() : companyCode;
        }
        return companyCode;
    }

    public String getMainLedgerAccount() {
        if (isActiveFromExceeded() && nextEntity != null) {
            return nextEntity.getMainLedgerAccount() != null ? nextEntity.getMainLedgerAccount() : mainLedgerAccount;
        }
        return mainLedgerAccount;
    }

    public String getVatCode() {
        if (isActiveFromExceeded() && nextEntity != null) {
            return nextEntity.getVatCode() != null ? nextEntity.getVatCode() : vatCode;
        }
        return vatCode;
    }

    public String getInternalOrder() {
        if (isActiveFromExceeded() && nextEntity != null) {
            return nextEntity.getInternalOrder() != null ? nextEntity.getInternalOrder() : internalOrder;
        }
        return internalOrder;
    }

    public String getProfitCenter() {
        if (isActiveFromExceeded() && nextEntity != null) {
            return nextEntity.getProfitCenter() != null ? nextEntity.getProfitCenter() : profitCenter;
        }
        return profitCenter;
    }

    public String getBalanceProfitCenter() {
        if (isActiveFromExceeded() && nextEntity != null) {
            return nextEntity.getBalanceProfitCenter() != null ? nextEntity.getBalanceProfitCenter() : balanceProfitCenter;
        }
        return balanceProfitCenter;
    }

    public String getProject() {
        if (isActiveFromExceeded() && nextEntity != null) {
            return nextEntity.getProject() != null ? nextEntity.getProject() : project;
        }
        return project;
    }

    public String getOperationArea() {
        if (isActiveFromExceeded() && nextEntity != null) {
            return nextEntity.getOperationArea() != null ? nextEntity.getOperationArea() : operationArea;
        }
        return operationArea;
    }

    public LocalDateTime getActiveFrom() {
        return activeFrom;
    }

    public NextAccountingEntityDto getNextEntity() {
        return nextEntity;
    }

    public String getNamespace() {
        return namespace;
    }

    private boolean isActiveFromExceeded() {
        LocalDateTime referenceTime = paidAt != null ? paidAt : LocalDateTime.now();
        return activeFrom != null && activeFrom.isBefore(referenceTime);
    }
}
