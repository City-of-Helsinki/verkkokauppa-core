package fi.hel.verkkokauppa.order.api.data.accounting;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class OrderAccountingDto {

    private String orderId;
    private String namespace;

    private LocalDateTime createdAt;

    private List<OrderItemAccountingDto> items;

}
