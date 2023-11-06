package fi.hel.verkkokauppa.order.api.data.invoice.xml;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class SalesOrder {
    @JacksonXmlProperty(localName = "SenderId")
    private final String senderId = "ID395";
    @JacksonXmlProperty(localName = "Reference")
    private String reference;
    @JacksonXmlProperty(localName = "OriginalOrder")
    private String originalOrder;
    @JacksonXmlProperty(localName = "OrderType")
    private String orderType;
    @JacksonXmlProperty(localName = "SalesOrg")
    private String salesOrg;
    @JacksonXmlProperty(localName = "DistributionChannel")
    private final String distributionChannel = "10";
    @JacksonXmlProperty(localName = "Division")
    private final String division = "10";
    @JacksonXmlProperty(localName = "SalesOffice")
    private String salesOffice;
    @JacksonXmlProperty(localName = "SalesGroup")
    private String salesGroup;
    @JacksonXmlProperty(localName = "PONumber")
    private String poNumber;
    @JacksonXmlProperty(localName = "BillingBlock")
    private String billingBlock;
    @JacksonXmlProperty(localName = "SalesDistrict")
    private String salesDistrict;
    @JacksonXmlProperty(localName = "HiddenText")
    private String hiddenText;
    @JacksonXmlProperty(localName = "BillText")
    private String billText;
    @JacksonXmlProperty(localName = "ReferenceText")
    private String referenceText;
    @JacksonXmlProperty(localName = "PMNTTERM")
    private String pmntterm;
    @JacksonXmlProperty(localName = "OrderReason")
    private String orderReason;
    @JacksonXmlProperty(localName = "BillingDate")
    @JsonFormat(pattern = "yyyyMMdd")
    private LocalDate billingDate;
    @JacksonXmlProperty(localName = "PricingDate")
    @JsonFormat(pattern = "yyyyMMdd")
    private LocalDate pricingDate;
    @JacksonXmlProperty(localName = "OrderParty")
    private Party orderParty;
    @JacksonXmlProperty(localName = "BillingParty1")
    private Party billingParty1;
    @JacksonXmlProperty(localName = "BillingParty2")
    private Party billingParty2;
    @JacksonXmlProperty(localName = "PayerParty")
    private Party payerParty;
    @JacksonXmlProperty(localName = "ShipToParty")
    private Party shipToParty;
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "LineItem")
    private List<LineItem> lineItems;
}
