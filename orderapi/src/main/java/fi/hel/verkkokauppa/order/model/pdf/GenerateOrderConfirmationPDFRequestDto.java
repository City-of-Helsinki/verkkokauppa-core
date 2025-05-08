package fi.hel.verkkokauppa.order.model.pdf;

import fi.hel.verkkokauppa.common.rest.dto.payment.PaymentDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import lombok.Data;

import java.util.List;

@Data
public class GenerateOrderConfirmationPDFRequestDto {
    private String orderId;
    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;
    private PaymentDto payment;

    private String merchantName;
    private String merchantStreetAddress;
    private String merchantZipCode;
    private String merchantCity;
    private String merchantPhoneNumber;
    private String merchantEmail;
    private String merchantBusinessId;

    private List<OrderItemDto> items;
}
