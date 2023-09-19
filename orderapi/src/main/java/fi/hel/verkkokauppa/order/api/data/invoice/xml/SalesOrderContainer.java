package fi.hel.verkkokauppa.order.api.data.invoice.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import javax.xml.XMLConstants;
import java.util.List;

@Data
@JacksonXmlRootElement(localName = "SBO_SalesOrderContainer")
public class SalesOrderContainer {
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xsi")
    private String schemaInstanceUri = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "SBO_SalesOrder")
    private List<SalesOrder> salesOrders;
}
