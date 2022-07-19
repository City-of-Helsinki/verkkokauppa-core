package fi.hel.verkkokauppa.order.api.data.invoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceDto {
    String orderId;
    String userId;
    // Model data
    String businessId;
    String name;
    String address;
    String postcode;
    String city;
    String ovtId;
}
