package fi.hel.verkkokauppa.order.api.data.accounting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public interface AccountingSlipMixInDto {

    @JsonIgnore
    String getAccountingSlipId();

    @JacksonXmlProperty(localName = "SenderId")
    String getSenderId();

}
