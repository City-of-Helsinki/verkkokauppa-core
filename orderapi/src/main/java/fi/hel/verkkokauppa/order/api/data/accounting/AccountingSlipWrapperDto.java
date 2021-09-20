package fi.hel.verkkokauppa.order.api.data.accounting;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlNs;

@Data
@JacksonXmlRootElement(localName = "SBO_SimpleAccountingContainer")
public class AccountingSlipWrapperDto {

    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xsi")
    private String schemaInstanceUri = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;

    @JacksonXmlProperty(localName = "SBO_SimpleAccounting")
    private AccountingSlipDto accountingSlip;

    public AccountingSlipWrapperDto(AccountingSlipDto accountingSlip) {
        this.accountingSlip = accountingSlip;
    }

}
