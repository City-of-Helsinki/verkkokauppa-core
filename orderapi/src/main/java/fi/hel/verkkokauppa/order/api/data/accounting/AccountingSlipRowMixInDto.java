package fi.hel.verkkokauppa.order.api.data.accounting;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface AccountingSlipRowMixInDto {

    @JsonIgnore
    String getAccountingSlipId();

    @JsonIgnore
    String getAccountingSlipRowId();

}
