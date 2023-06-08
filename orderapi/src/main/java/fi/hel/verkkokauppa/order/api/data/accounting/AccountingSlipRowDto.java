package fi.hel.verkkokauppa.order.api.data.accounting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import fi.hel.verkkokauppa.order.constants.AccountingRowTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonPropertyOrder({"TaxCode", "AmountInDocumentCurrency", "BaseAmount", "LineText", "GLAccount",
        "ProfitCenter", "OrderItemNumber", "WBS_Element", "FunctionalArea"})
public class AccountingSlipRowDto {

    private String accountingSlipRowId;

    private String accountingSlipId;

    @JacksonXmlProperty(localName = "TaxCode")
    private String taxCode;

    @JacksonXmlProperty(localName = "AmountInDocumentCurrency")
    private String amountInDocumentCurrency;

    @JacksonXmlProperty(localName = "BaseAmount")
    private String baseAmount;

    private String vatAmount;

    @JacksonXmlProperty(localName = "LineText")
    private String lineText;

    @JacksonXmlProperty(localName = "GLAccount")
    private String glAccount;

    @JacksonXmlProperty(localName = "ProfitCenter")
    private String profitCenter;

    private String balanceProfitCenter;

    @JacksonXmlProperty(localName = "OrderItemNumber")
    private String orderItemNumber;

    @JacksonXmlProperty(localName = "WBS_Element")
    private String wbsElement;

    @JacksonXmlProperty(localName = "FunctionalArea")
    private String functionalArea;

    private AccountingRowTypeEnum rowType;

    @JsonIgnore
    public Double getAmountInDocumentCurrencyAsDouble() {
        return Double.parseDouble(amountInDocumentCurrency.replace(",", "."));
    }

    @JsonIgnore
    public Double getBaseAmountAsDouble() {
        return Double.parseDouble(baseAmount.replace(",", "."));
    }

    @JsonIgnore
    public Double getVatAmountAsDouble() {
        return Double.parseDouble(vatAmount.replace(",", "."));
    }

    public AccountingSlipRowDto(AccountingSlipRowDto other) {
        this.accountingSlipRowId = other.getAccountingSlipRowId();
        this.accountingSlipId = other.getAccountingSlipId();
        this.taxCode = other.getTaxCode();
        this.amountInDocumentCurrency = other.getAmountInDocumentCurrency();
        this.baseAmount = other.getBaseAmount();
        this.vatAmount = other.getVatAmount();
        this.lineText = other.getLineText();
        this.glAccount = other.getGlAccount();
        this.profitCenter = other.getProfitCenter();
        this.balanceProfitCenter = other.getBalanceProfitCenter();
        this.orderItemNumber = other.getOrderItemNumber();
        this.wbsElement = other.getWbsElement();
        this.functionalArea = other.getFunctionalArea();
        this.rowType = other.getRowType();
    }
}
