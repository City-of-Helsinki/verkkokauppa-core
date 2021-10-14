package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemDto;

import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.model.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class CreateSubscriptionsFromOrderCommand {

	private final CreateSubscriptionCommand createSubscriptionCommand;

	@Autowired
	public CreateSubscriptionsFromOrderCommand(
			CreateSubscriptionCommand createSubscriptionCommand
	) {
		this.createSubscriptionCommand = createSubscriptionCommand;
	}

	public Set<String> createFromOrder(OrderAggregateDto orderAggregateDto) {
		if (!canCreateFromOrder(orderAggregateDto)) {
			throw new IllegalArgumentException("Can't create subscription order from this order.");
		}

		Set<String> idList = new HashSet<>();

		for (OrderItemDto orderItemDto : orderAggregateDto.getItems()) {
			if (!canCreateFromOrderItem(orderItemDto)) {
				continue;
			}

			SubscriptionDto subscriptionDto = new SubscriptionDto();

			copyOrderFieldsToSubscription(orderAggregateDto.getOrder(), subscriptionDto);
			copyOrderItemFieldsToSubscription(orderItemDto, subscriptionDto);
			// TODO refactor? Set billing cycles count
			subscriptionDto.setNumberOfBillingCycles(orderAggregateDto.getNumberOfBillingCycles());
			String id = createSubscriptionCommand.create(subscriptionDto);
			idList.add(id);
		}

		return idList;
	}

	private void copyOrderFieldsToSubscription(OrderDto order, SubscriptionDto subscriptionDto) {
		// User fields
		subscriptionDto.setUser(order.getUser());
		subscriptionDto.setNamespace(order.getNamespace());
		// Customer fields
		subscriptionDto.setCustomerEmail(order.getCustomerEmail());
		subscriptionDto.setCustomerPhone(order.getCustomerPhone());
		subscriptionDto.setCustomerFirstName(order.getCustomerFirstName());
		subscriptionDto.setCustomerLastName(order.getCustomerLastName());

		// Adds relation to order
		subscriptionDto.setRelatedOrderIds(new HashSet<>() {{ add(order.getOrderId()); }});
	}

	private void copyOrderItemFieldsToSubscription(OrderItemDto orderItem, SubscriptionDto subscriptionDto) {
		subscriptionDto.setProductId(orderItem.getProductId());
		subscriptionDto.setProductName(orderItem.getProductName());
		subscriptionDto.setQuantity(orderItem.getQuantity());
		subscriptionDto.setPeriodFrequency(orderItem.getPeriodFrequency());
		subscriptionDto.setPeriodUnit(orderItem.getPeriodUnit());
		//TODO
		subscriptionDto.setOrderItemStartDate(orderItem.getStartDate());
	}

	private boolean canCreateFromOrder(OrderAggregateDto orderAggregateDto) {
		return orderAggregateDto.getOrder().getType().equals(OrderType.SUBSCRIPTION);
	}

	private boolean canCreateFromOrderItem(OrderItemDto item) {
		return item.getPeriodUnit() != null && item.getPeriodFrequency() != null;
	}
}
