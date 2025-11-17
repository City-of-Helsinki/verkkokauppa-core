package fi.hel.verkkokauppa.order.api.data.invoice.xml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LineItem {
    @JsonIgnore
    private String orderItemId;
    @JacksonXmlProperty(localName = "GroupingFactor")
    private String groupingFactor;
    @JacksonXmlProperty(localName = "Material")
    private String material;
    @JacksonXmlProperty(localName = "MaterialDescription")
    private String materialDescription;
    @JacksonXmlProperty(localName = "Quantity")
    private String quantity;
    @JacksonXmlProperty(localName = "Unit")
    private final String unit = "kpl";
    @JacksonXmlProperty(localName = "NetPrice")
    private String netPrice;
    @JacksonXmlProperty(localName = "TaxCode")
    private String taxCode;
    @JacksonXmlProperty(localName = "MaterialSalesText")
    private String materialSalesText;
    @JacksonXmlProperty(localName = "LineTextL1")
    private String LineTextL1;
    @JacksonXmlProperty(localName = "LineTextL2")
    private String LineTextL2;
    @JacksonXmlProperty(localName = "LineTextL3")
    private String LineTextL3;
    @JacksonXmlProperty(localName = "LineTextL4")
    private String LineTextL4;
    @JacksonXmlProperty(localName = "LineTextL5")
    private String LineTextL5;
    @JacksonXmlProperty(localName = "LineTextL6")
    private String LineTextL6;
    @JacksonXmlProperty(localName = "ProfitCenter")
    private String profitCenter;
    @JacksonXmlProperty(localName = "OrderItemNumber")
    private String orderItemNumber;
    @JacksonXmlProperty(localName = "WBS_Element")
    private String wbsElement;
    @JacksonXmlProperty(localName = "FunctionalArea")
    private String functionalArea;
}
