package fi.hel.verkkokauppa.order.api.data.invoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceDto {
    String orderId;
    String userId;
    // Model data
    String invoiceId;
    String businessId;
    String name;
    String address;
    String postcode;
    String city;
    String ovtId;
}
