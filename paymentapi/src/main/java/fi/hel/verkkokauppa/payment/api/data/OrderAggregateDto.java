package fi.hel.verkkokauppa.payment.api.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderAggregateDto {

    private OrderDto order;

    private List<OrderItemDto> items = new ArrayList<>();
}
