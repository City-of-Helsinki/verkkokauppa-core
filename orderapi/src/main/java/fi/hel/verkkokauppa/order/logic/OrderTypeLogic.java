package fi.hel.verkkokauppa.order.logic;

import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderTypeLogic {

	public boolean setOrderType(Order order, List<OrderItemDto> items) {
		String oldType = order.getType();

		if (items == null || items.isEmpty()) {
			return false;
		}

		String newType = decideOrderTypeBasedOnItems(items);

		if (newType.equals(oldType)) {
			return false;
		}
		order.setType(newType);

		return true;
	}

	public String decideOrderTypeBasedOnItems(List<OrderItemDto> items) {
		boolean subscriptionItemsFound = (int)items.stream()
				.filter(item -> item.getPeriodFrequency() != null && item.getPeriodUnit() != null)
				.count() > 0;

		return subscriptionItemsFound ? OrderType.SUBSCRIPTION : OrderType.ORDER;
	}
}
