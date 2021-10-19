package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.api.data.transformer.SubscriptionItemMetaTransformer;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionItemMeta;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionItemMetaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class CreateSubscriptionsFromOrderCommand {

	private Logger log = LoggerFactory.getLogger(CreateSubscriptionsFromOrderCommand.class);

	@Autowired
	private SubscriptionItemMetaTransformer subscriptionItemMetaTransformer;

	@Autowired
	private SubscriptionItemMetaRepository subscriptionItemMetaRepository;

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
			copyOrderItemMetaFieldsToSubscription(orderItemDto, subscriptionDto);
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
		// Order item data
		subscriptionDto.setOrderItemId(orderItem.getOrderItemId());
		// Product data
		subscriptionDto.setProductId(orderItem.getProductId());
		subscriptionDto.setProductName(orderItem.getProductName());
		subscriptionDto.setQuantity(orderItem.getQuantity());
		// Period data
		subscriptionDto.setPeriodFrequency(orderItem.getPeriodFrequency());
		subscriptionDto.setPeriodUnit(orderItem.getPeriodUnit());
		subscriptionDto.setPeriodCount(orderItem.getPeriodCount());
		// Price data
		subscriptionDto.setPriceNet(orderItem.getPriceNet());
		subscriptionDto.setPriceVat(orderItem.getPriceVat());
		subscriptionDto.setPriceTotal(orderItem.getRowPriceTotal());
		// Date data
		subscriptionDto.setStartDate(orderItem.getStartDate());
		subscriptionDto.setBillingStartDate(orderItem.getBillingStartDate());
	}

	private void copyOrderItemMetaFieldsToSubscription(OrderItemDto orderItem, SubscriptionDto subscriptionDto) {
		if (!orderItem.getMeta().isEmpty()) {
			orderItem.getMeta().stream().forEach(meta -> {
				SubscriptionItemMeta subscriptionItemMeta = subscriptionItemMetaTransformer.transformToEntity(meta);
				SubscriptionItemMeta createdSubscriptionItemMeta = subscriptionItemMetaRepository.save(subscriptionItemMeta);
				log.debug("created new subscriptionItemMeta " + createdSubscriptionItemMeta.getOrderItemMetaId() + " from createdSubscriptionItemMeta: " + meta.getOrderItemMetaId());
			});
		}
	}

//	        if (!metas.isEmpty()) {
//		dto.getItems().stream().forEach(item -> {
//			List<OrderItemMeta> itemMetas = metas.stream()
//					.filter(meta -> meta.getOrderItemId().equals(item.getOrderItemId()))
//					.collect(Collectors.toList());
//
//			List<OrderItemMetaDto> itemMetaDtos = itemMetas.stream()
//					.map(itemMeta -> orderItemMetaTransformer.transformToDto(itemMeta))
//					.collect(Collectors.toList());
//
//			item.setMeta(itemMetaDtos);
//		});
//	}

	private boolean canCreateFromOrder(OrderAggregateDto orderAggregateDto) {
		return orderAggregateDto.getOrder().getType().equals(OrderType.SUBSCRIPTION);
	}

	private boolean canCreateFromOrderItem(OrderItemDto item) {
		return item.getPeriodUnit() != null && item.getPeriodFrequency() != null;
	}
}
