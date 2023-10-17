package fi.hel.verkkokauppa.payment.testing.data;

import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
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

    private FlowStepDto flowSteps;

    private OrderPaymentMethodDto paymentMethod;

}
