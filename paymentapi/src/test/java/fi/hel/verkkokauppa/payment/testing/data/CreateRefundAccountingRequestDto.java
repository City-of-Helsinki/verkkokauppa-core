package fi.hel.verkkokauppa.payment.testing.data;


import lombok.Data;

import java.util.List;

@Data
public class CreateRefundAccountingRequestDto {

    private String refundId;
    private String orderId;
    private List<ProductAccountingDto> dtos;

}
