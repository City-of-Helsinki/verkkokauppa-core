package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.rest.RestWebHookService;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.subscription.*;
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
import org.json.JSONObject;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Validated
public class SubscriptionController {
	private Logger log = LoggerFactory.getLogger(SubscriptionController.class);

	@Autowired
	private OrderService orderService;

	@Autowired
	private SubscriptionService subscriptionService;
	@Autowired
	private RestServiceClient restServiceClient;

	private final CreateSubscriptionCommand createSubscriptionCommand;
	private final GetSubscriptionQuery getSubscriptionQuery;
	private final SearchSubscriptionQuery searchSubscriptionQuery;
	private final CreateSubscriptionsFromOrderCommand createSubscriptionsFromOrderCommand;
	private final CancelSubscriptionCommand cancelSubscriptionCommand;
	private final UpdateSubscriptionCommand updateSubscriptionCommand;

	@Autowired
	private RestWebHookService restWebHookService;

	@Autowired
	private SaveHistoryService saveHistoryService;

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
			final Subscription subscription = cancelSubscriptionCommand.cancel(id, userId, SubscriptionCancellationCause.CUSTOMER_CANCELLED);
			return ResponseEntity.ok(getSubscriptionQuery.mapToDto(subscription));
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
		return subscriptionService.setSubscriptionCardInfoInternal(dto, true);
	}

	@PostMapping(value = "/subscription/set-card-token-event", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> setSubscriptionCardTokenEvent(@RequestBody SubscriptionMessage message) {
		ResponseEntity<Void>  tokenUpdateResponse = subscriptionService.setSubscriptionCardInfoInternal(UpdatePaymentCardInfoRequest.fromSubscriptionMessage(message), false);
		// Save update card event to history
		message.setNamespace("internal");
		saveHistoryService.saveSubscriptionMessageHistory(message);
		return tokenUpdateResponse;
	}


	@PostMapping(value = "/subscription/payment-failed-event", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Boolean> paymentFailedEventCallback(@RequestBody PaymentMessage message) {
		boolean updated = Boolean.FALSE;
		log.debug("subscription-api received PAYMENT_FAILED event for paymentId: " + message.getPaymentId());
		Order order = orderService.findByIdValidateByUser(message.getOrderId(), message.getUserId());

		Subscription subscription = getSubscriptionQuery.findByIdValidateByUser(order.getSubscriptionId(), message.getUserId());

		try {
			JSONObject result = subscriptionService.sendSubscriptionPaymentFailedEmail(subscription.getSubscriptionId());
		} catch (Exception e) {
			log.error("Error sending paymentFailedEmail for subscription {}", subscription.getSubscriptionId(), e);
		}

		if (subscription.getEndDate().isAfter(LocalDateTime.now())) {
			updated = Boolean.TRUE;
			cancelSubscriptionCommand.cancel(
					subscription.getSubscriptionId(),
					subscription.getUser(),
					SubscriptionCancellationCause.EXPIRED
			);
		}
		saveHistoryService.savePaymentMessageHistory(message);
		return ResponseEntity.ok(updated);
	}

	@PostMapping(value = "/subscription/payment-paid-event", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<SubscriptionIdsDto> paymentPaidEventCallback(@RequestBody PaymentMessage message) {
		log.debug("subscription-api received PAYMENT_PAID event for paymentId: " + message.getPaymentId());

		SubscriptionIdsDto dto = createSubscriptionsFromOrderId(message.getOrderId(), message.getUserId()).getBody();
		// TODO Method invocation 'getSubscriptionIds' may produce 'NullPointerException'
		subscriptionService.afterFirstPaymentPaidEventActions(dto.getSubscriptionIds(), message);
		saveHistoryService.savePaymentMessageHistory(message);
		return ResponseEntity.ok().body(dto);
	}

	@PostMapping(value = "/subscription/payment-paid-webhook", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> paymentPaidWebhook(@RequestBody PaymentMessage message) {

		try {
			// This row validates that message contains authorization to order.
			orderService.findByIdValidateByUser(message.getOrderId(), message.getUserId());
			saveHistoryService.savePaymentMessageHistory(message);
			return restWebHookService.postCallWebHook(message.toCustomerWebHook(), ServiceConfigurationKeys.MERCHANT_PAYMENT_WEBHOOK_URL, message.getNamespace());
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

	@PostMapping(value = "/subscription/payment-update-card", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> paymentCardUpdate(@RequestBody PaymentMessage message) {

		try {
			// This row validates that message contains authorization to order.
			Order order = orderService.findByIdValidateByUser(message.getOrderId(), message.getUserId());
			// Find subscription with subscription id from order and user id from message
			Subscription subscription = subscriptionService.findByIdValidateByUser(order.getSubscriptionId(), message.getUserId());

			// Update given card info to subscription.
			UpdatePaymentCardInfoRequest updateRequest = subscriptionService.getUpdatePaymentCardInfoRequest(
					subscription.getSubscriptionId(),
					message
			);

			ResponseEntity<Void> response = subscriptionService.setSubscriptionCardInfoInternal(
					updateRequest,
					false // Encrypted in message
			);
			saveHistoryService.savePaymentMessageHistory(message);
			return response;
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("updating card info to orderId: {} failed " , message.getOrderId(), e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-update-payment-card-renewal-event", "failed to update subscription card info for order with id [" + message.getOrderId() + "]")
			);
		}
	}

	@PostMapping(value = "/subscription/subscription-cancelled-webhook", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> subscriptionCancelledWebhook(@RequestBody SubscriptionMessage message) {
		try {
			saveHistoryService.saveSubscriptionMessageHistory(message);
			return restWebHookService.postCallWebHook(message.toCustomerWebHook(), ServiceConfigurationKeys.MERCHANT_SUBSCRIPTION_WEBHOOK_URL, message.getNamespace());
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("sending webhook data failed, subscriptionId: " + message.getSubscriptionId(), e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-call-subscription-cancelled-webhook", "failed to call subscription cancelled webhook for subscription with id [" + message.getSubscriptionId() + "]")
			);
		}
	}

	@GetMapping(value = "/subscription-get-by-order-id", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<SubscriptionDtoListWrapper> listSubscriptions(
			@RequestParam(value = "orderId") String orderId,
			@RequestParam(value = "userId") String userId
	) {
		try {
			List<Subscription> subscriptions = subscriptionService.findAllByOrderIdAndUser(orderId, userId);
			List<SubscriptionDto> subscriptionDtos = subscriptions
					.stream()
					.map(getSubscriptionQuery::mapToDto)
					.peek(subscriptionDto -> subscriptionDto.setMeta(
							subscriptionService.findMetasBySubscriptionId(subscriptionDto.getSubscriptionId())
					)).collect(Collectors.toList());

			return ResponseEntity.ok(new SubscriptionDtoListWrapper(subscriptionDtos));
		} catch (CommonApiException cae) {
			throw cae;
		} catch(EntityNotFoundException e) {
			log.error("Exception on listing Subscription order", e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}
}
