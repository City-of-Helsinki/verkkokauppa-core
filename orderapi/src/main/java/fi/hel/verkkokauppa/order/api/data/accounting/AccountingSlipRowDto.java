package fi.hel.verkkokauppa.order.api.data.accounting;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({ "TaxCode", "AmountInDocumentCurrency", "BaseAmount", "LineText", "GLAccount",
        "ProfitCenter", "OrderItemNumber", "WBS_Element", "FunctionalArea" })
public class AccountingSlipRowDto {

    private String accountingSlipRowId;

    private String accountingSlipId;

    @JacksonXmlProperty(localName = "TaxCode")
    private String taxCode;

    @JacksonXmlProperty(localName = "AmountInDocumentCurrency")
    private String amountInDocumentCurrency;

    @JacksonXmlProperty(localName = "BaseAmount")
    private String baseAmount;

    @JacksonXmlProperty(localName = "LineText")
    private String lineText;

    @JacksonXmlProperty(localName = "GLAccount")
    private String glAccount;

    @JacksonXmlProperty(localName = "ProfitCenter")
    private String profitCenter;

    @JacksonXmlProperty(localName = "OrderItemNumber")
    private String orderItemNumber;

    @JacksonXmlProperty(localName = "WBS_Element")
    private String wbsElement;

    @JacksonXmlProperty(localName = "FunctionalArea")
    private String functionalArea;

}
