package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.order.api.data.OrderItemMetaDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderItemMetaTransformer;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionCancellationCause;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionItemMeta;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemMetaRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionItemMetaRepository;
import fi.hel.verkkokauppa.order.service.order.OrderItemMetaService;
import fi.hel.verkkokauppa.order.service.order.OrderItemService;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CreateOrderFromSubscriptionCommand {

	private Logger log = LoggerFactory.getLogger(CreateOrderFromSubscriptionCommand.class);

	@Autowired
	private OrderService orderService;
	@Autowired
	private SubscriptionService subscriptionService;

	@Autowired
	private OrderItemService orderItemService;

	@Autowired
	private OrderItemMetaService orderItemMetaService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderItemMetaTransformer orderItemMetaTransformer;

	@Autowired
	private OrderItemMetaRepository orderItemMetaRepository;

	@Autowired
	private SubscriptionItemMetaRepository subscriptionItemMetaRepository;

	@Autowired
	private GetSubscriptionQuery getSubscriptionQuery;

	@Autowired
	private CancelSubscriptionCommand cancelSubscriptionCommand;

	public String createFromSubscription(SubscriptionDto subscriptionDto) {
		String namespace = subscriptionDto.getNamespace();
		String user = subscriptionDto.getUser();

		String subscriptionId = subscriptionDto.getSubscriptionId();

		String activeOrderFromSubscription = hasDuplicateOrder(subscriptionId, user);

		if (activeOrderFromSubscription != null) {
			return activeOrderFromSubscription;
		}

		Subscription subscription = getSubscriptionQuery.findByIdValidateByUser(subscriptionDto.getSubscriptionId(), user);

		// Returns null orderId if subscription card is expired
		if (subscriptionService.isCardExpired(subscription)) {
			subscriptionService.triggerSubscriptionRenewValidationFailedEvent(subscription);
			return null;
		}

		boolean hasRightToPurchase = orderService.validateRightOfPurchase(subscriptionDto.getOrderId(), user, namespace);
		// Returns null orderId if subscription right of purchase is false.
		if (!hasRightToPurchase) {
			log.info("subscription-renewal-no-right-of-purchase {}", subscriptionDto.getSubscriptionId());
			cancelSubscriptionCommand.cancel(subscription.getSubscriptionId(), subscription.getUser(), SubscriptionCancellationCause.NO_RIGHT_OF_PURCHASE);
			return null;
		}

		Order order = orderService.createByParams(namespace, user);
		order.setType(OrderType.ORDER);

		orderService.setStartDateAndCalculateNextEndDate(order, subscription, subscription.getEndDate());
		copyCustomerInfoFromSubscription(subscriptionDto, order);
		orderService.setTotals(order, subscriptionDto.getPriceNet(), subscriptionDto.getPriceVat(), subscriptionDto.getPriceGross());

		String orderId = order.getOrderId();
		String orderItemId = createOrderItemFieldsFromSubscription(orderId, subscriptionDto);
		copyOrderItemMetaFieldsFromSubscription(subscriptionDto, orderId, orderItemId);

		orderService.confirm(order);
		orderRepository.save(order);

		orderService.linkToSubscription(orderId, order.getUser(), subscriptionDto.getSubscriptionId());

		return orderId;
	}

	public String hasDuplicateOrder(String subscriptionId, String user) {
		try {

			Subscription subscription = getSubscriptionQuery.findByIdValidateByUser(subscriptionId, user);
			Order lastOrder = orderService.getLatestOrderWithSubscriptionId(subscriptionId);
			if (lastOrder != null) {
				// order endDate greater than current subscription endDate
				if (hasActiveSubscriptionOrder(subscription, lastOrder)) {
					return lastOrder.getOrderId();
				}
			}
			return null;
		} catch (Exception e) {
			//
			log.info(String.valueOf(e));
			return null;
		}
	}

	public boolean hasActiveSubscriptionOrder(Subscription subscription, Order lastOrder) {
		LocalDateTime subscriptionEndDate = subscription.getEndDate();
		LocalDateTime lastOrderEndDate = lastOrder.getEndDate();
		// Prevents NPO
		if (subscriptionEndDate == null || lastOrderEndDate == null) {
			return false;
		}

		// order endDate greater than current subscription endDate (isAfter)
		return lastOrderEndDate.isAfter(subscriptionEndDate);
	}

	private void copyCustomerInfoFromSubscription(SubscriptionDto subscriptionDto, Order order) {
		String customerFirstName = subscriptionDto.getCustomerFirstName();
		String customerLastName = subscriptionDto.getCustomerLastName();
		String customerEmail = subscriptionDto.getCustomerEmail();
		String customerPhone = subscriptionDto.getCustomerPhone();
		orderService.setCustomer(order, customerFirstName, customerLastName, customerEmail, customerPhone);
	}

	private String createOrderItemFieldsFromSubscription(String orderId, SubscriptionDto subscriptionDto) {
		return orderItemService.addItem(
				orderId,
				subscriptionDto.getProductId(),
				subscriptionDto.getProductName(),
				subscriptionDto.getQuantity(),
				subscriptionDto.getUnit(),
				subscriptionDto.getPriceNet(),
				subscriptionDto.getPriceVat(),
				subscriptionDto.getPriceGross(),
				subscriptionDto.getVatPercentage(),
				subscriptionDto.getPriceNet(),
				subscriptionDto.getPriceVat(),
				subscriptionDto.getPriceGross(),
				null,
				null,
				null,
				subscriptionDto.getBillingStartDate(),
				subscriptionDto.getEndDate()
		);

	}

	private void copyOrderItemMetaFieldsFromSubscription(SubscriptionDto subscriptionDto, String orderId, String orderItemId) {
		List<SubscriptionItemMeta> subscriptionMeta = subscriptionItemMetaRepository.findBySubscriptionId(subscriptionDto.getSubscriptionId());
		if (!subscriptionMeta.isEmpty()) {
			subscriptionMeta.forEach(meta -> {
				OrderItemMetaDto orderItemMeta = new OrderItemMetaDto(
						null,
						orderItemId,
						orderId,
						meta.getKey(),
						meta.getValue(),
						meta.getLabel(),
						meta.getVisibleInCheckout(),
						meta.getOrdinal()
				);

				String createdOrderItemMetaId = orderItemMetaService.addItemMeta(orderItemMeta);

				log.debug("created new orderItemMeta " + createdOrderItemMetaId + " from subscriptionItemMeta: " + meta.getOrderItemMetaId());
			});
		}
	}

}
