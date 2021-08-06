package fi.hel.verkkokauppa.order.api.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderAggregateDto {

    @NotNull(message = "order required")
    private OrderDto order;

    @NotNull(message = "items required")
    private List<OrderItemDto> items = new ArrayList<>();
}
