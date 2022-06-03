package fi.hel.verkkokauppa.payment.api.data;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
public class PaymentFilterDto {
    String filterId;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    @NotBlank(message = "namespace cant be blank")
    String namespace;
    @NotBlank(message = "referenceId cant be blank")
    String referenceId;
    @NotBlank(message = "referenceType cant be blank")
    String referenceType;
    @NotBlank(message = "type cant be blank")
    String type;
    String value;
}
