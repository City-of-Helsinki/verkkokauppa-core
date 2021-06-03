package fi.hel.verkkokauppa.order.api.data;

import java.util.List;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;

public class OrderDto {
    private Order order;
    private List<OrderItem> items;


    public OrderDto() {}

    public OrderDto(Order order, List<OrderItem> items) {
        this.order = order;
        this.items = items;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
    
}