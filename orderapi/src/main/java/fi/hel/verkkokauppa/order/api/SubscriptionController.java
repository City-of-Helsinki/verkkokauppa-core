package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.rest.RestWebHookService;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionCriteria;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionIdsDto;
import fi.hel.verkkokauppa.order.api.data.subscription.UpdatePaymentCardInfoRequest;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionCancellationCause;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionStatus;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.service.subscription.CancelSubscriptionCommand;
import fi.hel.verkkokauppa.order.service.subscription.CreateSubscriptionCommand;
import fi.hel.verkkokauppa.order.service.subscription.CreateSubscriptionsFromOrderCommand;
import fi.hel.verkkokauppa.order.service.subscription.GetSubscriptionQuery;
import fi.hel.verkkokauppa.order.service.subscription.SearchSubscriptionQuery;
import fi.hel.verkkokauppa.order.service.subscription.SubscriptionService;
import fi.hel.verkkokauppa.order.service.subscription.UpdateSubscriptionCommand;
import fi.hel.verkkokauppa.shared.exception.EntityNotFoundException;
import fi.hel.verkkokauppa.shared.model.impl.IdWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

@RestController
@Validated
public class SubscriptionController {
	private Logger log = LoggerFactory.getLogger(SubscriptionController.class);

	@Autowired
	private OrderService orderService;

	@Autowired
	private SubscriptionService subscriptionService;

	private final CreateSubscriptionCommand createSubscriptionCommand;
	private final GetSubscriptionQuery getSubscriptionQuery;
	private final SearchSubscriptionQuery searchSubscriptionQuery;
	private final CreateSubscriptionsFromOrderCommand createSubscriptionsFromOrderCommand;
	private final CancelSubscriptionCommand cancelSubscriptionCommand;
	private final UpdateSubscriptionCommand updateSubscriptionCommand;

	@Autowired
	private RestWebHookService restWebHookService;

	@Autowired
	public SubscriptionController(
			CreateSubscriptionCommand createSubscriptionCommand,
			GetSubscriptionQuery getSubscriptionQuery,
			SearchSubscriptionQuery searchSubscriptionQuery,
			CreateSubscriptionsFromOrderCommand createSubscriptionsFromOrderCommand,
			CancelSubscriptionCommand cancelSubscriptionCommand,
			UpdateSubscriptionCommand updateSubscriptionCommand) {
		this.createSubscriptionCommand = createSubscriptionCommand;
		this.createSubscriptionsFromOrderCommand = createSubscriptionsFromOrderCommand;
		this.getSubscriptionQuery = getSubscriptionQuery;
		this.searchSubscriptionQuery = searchSubscriptionQuery;
		this.cancelSubscriptionCommand = cancelSubscriptionCommand;
		this.updateSubscriptionCommand = updateSubscriptionCommand;
	}

	// TODO refactor path to indicate what is about to happen
    @GetMapping(value = "/subscription", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<SubscriptionDto> getSubscription(@RequestParam(value = "id") String id, @RequestParam(value = "userId") String userId) {
		try {
			final SubscriptionDto subscription = getSubscriptionQuery.getOneValidateByUser(id, userId);
			return ResponseEntity.ok(subscription);
		} catch (CommonApiException cae) {
			throw cae;
		} catch(EntityNotFoundException e) {
			log.error("Exception on getting Subscription order", e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	// TODO refactor path to indicate what is about to happen
	@PostMapping(value = "/subscription", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<IdWrapper> createSubscription(@RequestBody SubscriptionDto dto) {
		final String id = createSubscriptionCommand.create(dto);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new IdWrapper(id));
	}

	@PostMapping(value = "/subscription/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<SubscriptionDto> cancelSubscription(@RequestParam(value = "id") String id, @RequestParam(value = "userId") String userId) {
		try {
			cancelSubscriptionCommand.cancel(id, userId, SubscriptionCancellationCause.CUSTOMER_CANCELLED);

			final SubscriptionDto subscription = getSubscriptionQuery.getOneValidateByUser(id, userId);
			return ResponseEntity.ok(subscription);
		} catch (CommonApiException cae) {
			throw cae;
		} catch(EntityNotFoundException e) {
			log.error("Exception on cancelling Subscription order", e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@PostMapping(value = "/subscription/search/active", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<SubscriptionDto>> searchActive(
			@RequestBody SubscriptionCriteria criteria
	) {
		try {
			// It's possible to give other status in criteria, but we always want to search for active Subscription order.
			criteria.setStatus(SubscriptionStatus.ACTIVE);

			final List<SubscriptionDto> subscription = searchSubscriptionQuery.searchActive(criteria);
			return ResponseEntity.ok(subscription);
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("Exception on searching Subscription order", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PostMapping(value = "/subscription/create-from-order", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<SubscriptionIdsDto> createSubscriptionsFromOrder(@Valid @RequestBody OrderAggregateDto dto) {
		try {
			Set<String> idList = createSubscriptionsFromOrderCommand.createFromOrder(dto);
			SubscriptionIdsDto idListDto = SubscriptionIdsDto.builder().subscriptionIds(idList).build();

			return ResponseEntity.status(HttpStatus.CREATED).body(idListDto);
		} catch (Exception e) {
			log.error("Exception on creating Subscription order from order", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping(value = "/subscription/create-from-order", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<SubscriptionIdsDto> createSubscriptionsFromOrderId(@RequestParam(value = "orderId") String orderId,
																	  @RequestParam(value = "userId") String userId) {
		try {
			Order foundOrder = orderService.findByIdValidateByUser(orderId, userId);
			OrderAggregateDto dto = orderService.getOrderWithItems(foundOrder.getOrderId());
			Set<String> idList = createSubscriptionsFromOrderCommand.createFromOrder(dto);
			SubscriptionIdsDto idListDto = SubscriptionIdsDto.builder().subscriptionIds(idList).build();

			return ResponseEntity.status(HttpStatus.CREATED).body(idListDto);
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("Exception on creating Subscription order from order", e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-create-subscription-from-order",
							"Failed to create subscription from order [" + orderId + "]")
			);
		}
	}

	@GetMapping(value = "/subscription/get-card-token", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getSubscriptionCardToken(@RequestParam(value = "id") String id, @RequestParam(value = "userId") String userId) {
		try {
			SubscriptionDto subscription = getSubscriptionQuery.getOneValidateByUser(id, userId);
			String token = subscription.getPaymentMethodToken();

			return ResponseEntity.ok().body(token);
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("getting payment method token from subscription with id [" + id + "] failed", e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-get-payment-method-token-from-subscription",
							"getting payment method token from subscription with id [" + id + "] failed")
			);
		}
	}

	@PutMapping(value = "/subscription/set-card-token", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> setSubscriptionCardToken(@RequestBody UpdatePaymentCardInfoRequest dto) {
		return subscriptionService.setSubscriptionCardTokenInternal(dto, true);
	}


	@PostMapping(value = "/subscription/payment-failed-event", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> paymentFailedEventCallback(@RequestBody PaymentMessage message) {
		log.debug("subscription-api received PAYMENT_FAILED event for paymentId: " + message.getPaymentId());

		// TODO
		return null;
	}

	@PostMapping(value = "/subscription/payment-paid-event", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<SubscriptionIdsDto> paymentPaidEventCallback(@RequestBody PaymentMessage message) {
		log.debug("subscription-api received PAYMENT_PAID event for paymentId: " + message.getPaymentId());

		SubscriptionIdsDto dto = createSubscriptionsFromOrderId(message.getOrderId(), message.getUserId()).getBody();
		subscriptionService.afterFirstPaymentPaidEventActions(dto.getSubscriptionIds(), message);
		return ResponseEntity.ok().body(dto);
	}

	@PostMapping(value = "/subscription/payment-paid-webhook", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> paymentPaidWebhook(@RequestBody PaymentMessage message) {

		try {
			// This row validates that message contains authorization to order.
			orderService.findByIdValidateByUser(message.getOrderId(), message.getUserId());

			restWebHookService.setNamespace(message.getNamespace());
			return restWebHookService.postCallWebHook(message.toCustomerWebHook(), ServiceConfigurationKeys.MERCHANT_PAYMENT_WEBHOOK_URL);
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("sending webhook data failed, orderId: " + message.getOrderId(), e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-send-payment-paid-event", "failed to call payment paid webhook for order with id [" + message.getOrderId() + "]")
			);
		}
	}

	@PostMapping(value = "/subscription/subscription-cancelled-webhook", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> subscriptionCancelledWebhook(@RequestBody SubscriptionMessage message) {
		try {
			restWebHookService.setNamespace(message.getNamespace());
			return restWebHookService.postCallWebHook(message.toCustomerWebHook(), ServiceConfigurationKeys.MERCHANT_SUBSCRIPTION_WEBHOOK_URL);
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("sending webhook data failed, subscriptionId: " + message.getSubscriptionId(), e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-call-subscription-cancelled-webhook", "failed to call subscription cancelled webhook for order with id [" + message.getSubscriptionId() + "]")
			);
		}
	}
}
