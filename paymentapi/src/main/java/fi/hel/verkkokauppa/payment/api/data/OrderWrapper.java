package fi.hel.verkkokauppa.payment.api.data;

import java.util.ArrayList;
import java.util.List;

public class OrderWrapper {

	private OrderDto order;
	private List<OrderItemDto> items = new ArrayList<>();

	public OrderDto getOrder() {
		return order;
	}

	public void setOrder(OrderDto order) {
		this.order = order;
	}

	public List<OrderItemDto> getItems() {
		return items;
	}

	public void setItems(List<OrderItemDto> items) {
		this.items = items;
	}
}
