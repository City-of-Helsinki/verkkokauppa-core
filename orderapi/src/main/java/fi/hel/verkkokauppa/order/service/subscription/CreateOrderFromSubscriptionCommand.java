package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.util.ListUtil;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemMetaDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderItemMetaTransformer;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
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
import java.util.Objects;
import java.util.Optional;

@Service
public class CreateOrderFromSubscriptionCommand {

	private Logger log = LoggerFactory.getLogger(CreateOrderFromSubscriptionCommand.class);

	@Autowired
	private OrderService orderService;

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

	public String createFromSubscription(SubscriptionDto subscriptionDto) {
		String namespace = subscriptionDto.getNamespace();
		String user = subscriptionDto.getUser();
		// TODO create check for duplications!
		// Tarkista uusinta orderin olemassa olo ennen uuden uusinta orderin luontia
		try {

			List<OrderAggregateDto> orders = orderService.findBySubscription(subscriptionDto.getSubscriptionId());
			Subscription subscription = getSubscriptionQuery.findByIdValidateByUser(subscriptionDto.getSubscriptionId(), user);

			Optional<OrderAggregateDto> last = ListUtil.last(orders);
			if (last.isPresent()) {
				Order lastOrder = orderService.findById(last.get().getOrder().getOrderId());
				// order endDate greater than current subscription endDate
				// or subsciptionEndDate = Order endDate
				if (hasActiveSubscriptionOrder(subscription, lastOrder)) {
					return lastOrder.getOrderId();
				}
			}
		} catch (CommonApiException e) {
			//
			log.info(String.valueOf(e));
		}

		Order order = orderService.createByParams(namespace, user);
		order.setType(OrderType.ORDER);

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

	private boolean hasActiveSubscriptionOrder(Subscription subscription, Order lastOrder) {
		LocalDateTime subscriptionEndDate = subscription.getEndDate();
		LocalDateTime lastOrderEndDate = lastOrder.getEndDate();

		if (subscriptionEndDate == null || lastOrderEndDate == null) {
			return false;
		}

		return lastOrderEndDate.isAfter(subscriptionEndDate) || lastOrderEndDate.isEqual(subscriptionEndDate);

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
				subscriptionDto.getPeriodUnit(),
				subscriptionDto.getPeriodFrequency(),
				subscriptionDto.getPeriodCount(),
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
