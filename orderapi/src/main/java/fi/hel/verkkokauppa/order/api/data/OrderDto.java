package fi.hel.verkkokauppa.order.api.data;

import java.util.ArrayList;
import java.util.List;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderDto {

    private Order order;
    private List<OrderItem> items = new ArrayList<>();

    public OrderDto(Order order, List<OrderItem> items) {
        this.order = order;
        this.items = items;
    }
}