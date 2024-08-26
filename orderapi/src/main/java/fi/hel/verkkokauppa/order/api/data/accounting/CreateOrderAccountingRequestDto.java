package fi.hel.verkkokauppa.order.api.data.accounting;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class CreateOrderAccountingRequestDto {

    private String orderId;
    private String namespace;
    private String paidAt;
    private List<ProductAccountingDto> dtos;

}
