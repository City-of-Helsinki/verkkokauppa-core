package fi.hel.verkkokauppa.order.api.data.accounting;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class CreateRefundAccountingRequestDto {

    private String refundId;
    private String orderId;
    private List<ProductAccountingDto> dtos;

}
