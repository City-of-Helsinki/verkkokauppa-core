package fi.hel.verkkokauppa.order.api.data.invoice.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Party {
    @JacksonXmlProperty(localName = "SAPCustomerID")
    private String sapCustomerId;
    @JacksonXmlProperty(localName = "CustomerID")
    private String customerId;
    @JacksonXmlProperty(localName = "CustomerYID")
    private String customerYid;
    @JacksonXmlProperty(localName = "CustomerOVT")
    private String customerOvt;
    @JacksonXmlProperty(localName = "TemporaryAddress1")
    private String temporaryAddress1;
    @JacksonXmlProperty(localName = "TemporaryAddress2")
    private String temporaryAddress2;
    @JacksonXmlProperty(localName = "TemporaryPOCode")
    private String temporaryPoCode;
    @JacksonXmlProperty(localName = "TemporaryCity")
    private String temporaryCity;
    @JacksonXmlProperty(localName = "TemporaryPostalcode")
    private String temporaryPostalcode;
    @JacksonXmlProperty(localName = "PriorityName1")
    private String priorityName1;
    @JacksonXmlProperty(localName = "PriorityName2")
    private String priorityName2;
    @JacksonXmlProperty(localName = "PriorityName3")
    private String priorityName3;
    @JacksonXmlProperty(localName = "PriorityName4")
    private String priorityName4;
    @JacksonXmlProperty(localName = "PriorityAddress1")
    private String priorityAddress1;
    @JacksonXmlProperty(localName = "PriorityAddress2")
    private String priorityAddress2;
    @JacksonXmlProperty(localName = "PriorityPOCode")
    private String priorityPoCode;
    @JacksonXmlProperty(localName = "PriorityCity")
    private String priorityCity;
    @JacksonXmlProperty(localName = "PriorityPostalcode")
    private String priorityPostalcode;
}
