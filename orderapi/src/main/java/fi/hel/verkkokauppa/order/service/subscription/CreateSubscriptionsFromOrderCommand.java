package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.api.data.subscription.MerchantDto;
import fi.hel.verkkokauppa.order.api.data.subscription.ProductDto;
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

			String id = createSubscriptionCommand.create(subscriptionDto);
			idList.add(id);
		}

		return idList;
	}

	private void copyOrderFieldsToSubscription(OrderDto order, SubscriptionDto subscriptionDto) {
		MerchantDto merchant = new MerchantDto();
		merchant.setName(order.getUser());
		merchant.setNamespace(order.getNamespace());

		subscriptionDto.setRelatedOrderIds(new HashSet<>() {{ add(order.getOrderId()); }});
		subscriptionDto.setCustomerId(order.getCustomerEmail());
		subscriptionDto.setMerchant(merchant);
	}

	private void copyOrderItemFieldsToSubscription(OrderItemDto orderItem, SubscriptionDto subscriptionDto) {
		ProductDto product = new ProductDto();
		product.setId(orderItem.getProductId());
		product.setName(orderItem.getProductName());

		subscriptionDto.setProduct(product);
		subscriptionDto.setQuantity(orderItem.getQuantity());
		subscriptionDto.setStartDate(orderItem.getStartDate());
		subscriptionDto.setPeriodFrequency(orderItem.getPeriodFrequency());
		subscriptionDto.setPeriodUnit(orderItem.getPeriodUnit());
		//TODO Poista
//		SubscriptionDto.setPriceTotal(orderItem.getRowPriceTotal());
//		SubscriptionDto.setPriceVat(orderItem.getRowPriceVat());
//		SubscriptionDto.setPriceNet(orderItem.getRowPriceNet());
	}

	private boolean canCreateFromOrder(OrderAggregateDto orderAggregateDto) {
		return orderAggregateDto.getOrder().getType().equals(OrderType.SUBSCRIPTION);
	}

	private boolean canCreateFromOrderItem(OrderItemDto item) {
		return item.getPeriodUnit() != null && item.getPeriodFrequency() != null;
	}
}
