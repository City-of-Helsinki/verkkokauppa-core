package fi.hel.verkkokauppa.order.api.data;

import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionAggregateDto;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderAggregateDto extends SubscriptionAggregateDto {

    @NotNull(message = "order required")
    private OrderDto order;

    @NotNull(message = "items required")
    private List<OrderItemDto> items = new ArrayList<>();

}
