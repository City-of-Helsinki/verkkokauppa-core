package fi.hel.verkkokauppa.order.api.data.accounting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonPropertyOrder({ "SenderId", "CompanyCode", "DocumentType", "DocumentDate", "PostingDate", "Reference", "HeaderText", "CurrencyCode", "LineItem" })
public class AccountingSlipDto {

    private String accountingSlipId;

    // This is added only to the xml file
    @JsonIgnore
    @JacksonXmlProperty(localName = "SenderId")
    private String senderId = "ID378";

    @JacksonXmlProperty(localName = "CompanyCode")
    private String companyCode;

    @JacksonXmlProperty(localName = "DocumentType")
    private String documentType;

    @JacksonXmlProperty(localName = "DocumentDate")
    private String documentDate;

    @JacksonXmlProperty(localName = "PostingDate")
    private String postingDate;

    @JacksonXmlProperty(localName = "Reference")
    private String reference;

    @JacksonXmlProperty(localName = "HeaderText")
    private String headerText;

    @JacksonXmlProperty(localName = "CurrencyCode")
    private String currencyCode;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "LineItem")
    private List<AccountingSlipRowDto> rows;

    public AccountingSlipDto(String accountingSlipId, String companyCode, String documentType, String documentDate,
                             String postingDate, String reference, String headerText, String currencyCode,
                             List<AccountingSlipRowDto> rows) {
        this.accountingSlipId = accountingSlipId;
        this.companyCode = companyCode;
        this.documentType = documentType;
        this.documentDate = documentDate;
        this.postingDate = postingDate;
        this.reference = reference;
        this.headerText = headerText;
        this.currencyCode = currencyCode;
        this.rows = rows;
    }

}
