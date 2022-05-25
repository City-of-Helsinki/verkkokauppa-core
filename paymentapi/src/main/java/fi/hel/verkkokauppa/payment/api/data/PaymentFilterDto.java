package fi.hel.verkkokauppa.payment.api.data;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class PaymentFilterDto {
    @NotBlank(message = "referenceId cant be blank")
    String referenceId;
    @NotBlank(message = "type cant be blank")
    String type;
    @NotBlank(message = "value cant be blank")
    String value;
}
