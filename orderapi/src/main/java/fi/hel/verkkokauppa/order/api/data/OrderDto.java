package fi.hel.verkkokauppa.order.api.data;

import fi.hel.verkkokauppa.order.model.invoice.Invoice;
import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {

    private String orderId;
    private String subscriptionId;
    private String namespace;
    private String user;
    private LocalDateTime createdAt;
    private String status;
    private String type;
    private String priceNet;
    private String priceVat;
    private String priceTotal;
    private LocalDate accounted;

    @NotBlank(message = "firstname required")
    private String customerFirstName;

    @NotBlank(message = "lastname required")
    private String customerLastName;

    @Email(message = "email must be in correct format")
    @NotBlank(message = "email required")
    private String customerEmail;

    private String customerPhone;

    private Invoice invoice;

    private Long incrementId;

    private LocalDateTime lastValidPurchaseDateTime;

}