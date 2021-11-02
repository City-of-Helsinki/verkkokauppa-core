package fi.hel.verkkokauppa.order.api.data;

import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
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

    @Size(min = 1, message = "items required")
    private List<OrderItemDto> items = new ArrayList<>();

}
