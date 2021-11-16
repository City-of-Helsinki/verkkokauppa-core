package fi.hel.verkkokauppa.order.service.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemMetaDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderItemMetaTransformer;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionItemMeta;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemMetaRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionItemMetaRepository;
import fi.hel.verkkokauppa.order.service.order.OrderItemMetaService;
import fi.hel.verkkokauppa.order.service.order.OrderItemService;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.service.rightOfPurchase.OrderRightOfPurchaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

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

	public String createFromSubscription(SubscriptionDto subscriptionDto) throws JsonProcessingException {
		String namespace = subscriptionDto.getNamespace();
		String user = subscriptionDto.getUser();

		boolean hasRightToPurchase = orderService.validateRightOfPurchase(subscriptionDto.getOrderId(), user, namespace);
		// Returns null orderId if subscription right of purchase is false.
		if (!hasRightToPurchase) {
			log.info("subscription-renewal-no-right-of-purchase {}", subscriptionDto.getSubscriptionId());
			return null;
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
