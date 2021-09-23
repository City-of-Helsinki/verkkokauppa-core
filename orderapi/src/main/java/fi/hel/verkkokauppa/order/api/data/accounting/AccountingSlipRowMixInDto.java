package fi.hel.verkkokauppa.order.api.data.accounting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public interface AccountingSlipRowMixInDto {

    @JsonIgnore
    String getAccountingSlipId();

    @JsonIgnore
    String getAccountingSlipRowId();

    @JsonIgnore
    String getVatAmount();

    @JacksonXmlProperty(localName = "ProfitCenter")
    String getProfitCenter();

}
