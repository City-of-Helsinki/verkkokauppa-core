package fi.hel.verkkokauppa.message.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GenerateOrderConfirmationPDFRequestDto {
    private String orderId;
    private String createdAt;
    private List<OrderItemDto> items;
}
