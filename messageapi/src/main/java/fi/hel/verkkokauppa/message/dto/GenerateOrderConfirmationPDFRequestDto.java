package fi.hel.verkkokauppa.message.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GenerateOrderConfirmationPDFRequestDto {
    private String orderId;
    private PaymentDto payment;
    private MerchantDto merchant;
    private List<OrderItemDto> items;
}
