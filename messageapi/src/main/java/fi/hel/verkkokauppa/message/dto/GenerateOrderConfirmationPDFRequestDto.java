package fi.hel.verkkokauppa.message.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
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
