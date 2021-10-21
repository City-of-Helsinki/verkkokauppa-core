package fi.hel.verkkokauppa.order.api.data;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderAggregateDto {

    @NotNull(message = "order required")
    private OrderDto order;

    @NotNull(message = "items required")
    private List<OrderItemDto> items = new ArrayList<>();

}
