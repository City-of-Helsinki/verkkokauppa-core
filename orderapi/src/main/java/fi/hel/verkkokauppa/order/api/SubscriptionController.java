package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.SendEventService;
import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.util.EncryptorUtil;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.subscription.PaymentCardInfoDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionCriteria;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.api.data.subscription.UpdatePaymentCardInfoRequest;
import fi.hel.verkkokauppa.order.constants.SubscriptionUrlConstants;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionStatus;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.service.subscription.*;
import fi.hel.verkkokauppa.shared.exception.EntityNotFoundException;
import fi.hel.verkkokauppa.shared.model.impl.IdWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

@RestController
@Validated
@RequestMapping(SubscriptionUrlConstants.SUBSCRIPTION_API_ROOT)
public class SubscriptionController {

	@Value("${payment.card_token.encryption.password}")
	private String cardTokenEncryptionPassword;

	private Logger log = LoggerFactory.getLogger(SubscriptionController.class);

	@Autowired
	private OrderService orderService;

	@Autowired
	private SendEventService sendEventService;

	private final CreateSubscriptionCommand createSubscriptionCommand;
	//private final UpdateSubscriptionOrderCommand updateSubscriptionOrderCommand;
	private final GetSubscriptionQuery getSubscriptionQuery;
	private final SearchSubscriptionQuery searchSubscriptionQuery;
	private final CreateSubscriptionsFromOrderCommand createSubscriptionsFromOrderCommand;
	private final CancelSubscriptionCommand cancelSubscriptionCommand;
	private final UpdateSubscriptionCommand updateSubscriptionCommand;

	@Autowired
	public SubscriptionController(
			CreateSubscriptionCommand createSubscriptionCommand,
			//UpdateSubscriptionOrderCommand updateSubscriptionOrderCommand,
			GetSubscriptionQuery getSubscriptionQuery,
			SearchSubscriptionQuery searchSubscriptionQuery,
			CreateSubscriptionsFromOrderCommand createSubscriptionsFromOrderCommand,
			CancelSubscriptionCommand cancelSubscriptionCommand,
			UpdateSubscriptionCommand updateSubscriptionCommand) {
		this.createSubscriptionCommand = createSubscriptionCommand;
		this.createSubscriptionsFromOrderCommand = createSubscriptionsFromOrderCommand;
		//this.updateSubscriptionOrderCommand = updateSubscriptionOrderCommand;
		this.getSubscriptionQuery = getSubscriptionQuery;
		this.searchSubscriptionQuery = searchSubscriptionQuery;
		this.cancelSubscriptionCommand = cancelSubscriptionCommand;
		this.updateSubscriptionCommand = updateSubscriptionCommand;
	}

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
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

	@PostMapping(value = "/search/active", produces = MediaType.APPLICATION_JSON_VALUE)
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

	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<IdWrapper> createSubscription(@RequestBody SubscriptionDto dto) {
		final String id = createSubscriptionCommand.create(dto);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new IdWrapper(id));
	}

	@PostMapping(value = "/create-from-order", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Set<String>> createSubscriptionsFromOrder(@Valid @RequestBody OrderAggregateDto dto) {
		try {
			Set<String> idList = createSubscriptionsFromOrderCommand.createFromOrder(dto);

			return ResponseEntity.status(HttpStatus.CREATED)
					.body(idList);
		} catch (Exception e) {
			log.error("Exception on creating Subscription order from order", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping(value = "/create-from-order", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Set<String>> createSubscriptionsFromOrderId(@RequestParam(value = "orderId") String orderId,
																	  @RequestParam(value = "userId") String userId) {
		try {
			Order foundOrder = orderService.findByIdValidateByUser(orderId, userId);
			OrderAggregateDto dto = orderService.getOrderWithItems(foundOrder.getOrderId());
			Set<String> idList = createSubscriptionsFromOrderCommand.createFromOrder(dto);

			return ResponseEntity.status(HttpStatus.CREATED)
					.body(idList);
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

	@GetMapping(value = "/get-card-token", produces = MediaType.APPLICATION_JSON_VALUE)
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

	@PutMapping(value = "/set-card-token", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> setSubscriptionCardToken(@RequestBody UpdatePaymentCardInfoRequest dto) {
		String subscriptionId = dto.getSubscriptionId();
		String userId = dto.getUser();

		try {
			SubscriptionDto subscriptionDto = getSubscriptionQuery.getOneValidateByUser(subscriptionId, userId);
			PaymentCardInfoDto paymentCardInfoDto = dto.getPaymentCardInfoDto();

			String encryptedToken = EncryptorUtil.encryptValue(paymentCardInfoDto.getCardToken(), cardTokenEncryptionPassword);

			subscriptionDto.setPaymentMethodToken(encryptedToken);
			subscriptionDto.setPaymentMethodExpirationYear(paymentCardInfoDto.getExpYear());
			subscriptionDto.setPaymentMethodExpirationMonth(paymentCardInfoDto.getExpMonth());
			updateSubscriptionCommand.update(subscriptionId, subscriptionDto);

			return ResponseEntity.ok().build();
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("setting payment method token for subscription with id [" + subscriptionId + "] failed", e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-set-payment-method-token-for-subscription",
							"setting payment method token for subscription with id [" + subscriptionId + "] failed")
			);
		}
	}

	@PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> cancelSubscription(@RequestParam(value = "id") String id, @RequestParam(value = "userId") String userId) {
		try {
			cancelSubscriptionCommand.cancel(id, userId);
			return ResponseEntity.ok().build();
		} catch (CommonApiException cae) {
			throw cae;
		} catch(EntityNotFoundException e) {
			log.error("Exception on cancelling Subscription order", e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	/*@PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> updateSubscriptionOrder(@PathVariable("id") String id, @RequestBody SubscriptionOrderDto dto) {
		try {
			updateSubscriptionOrderCommand.update(id, dto);
			return ResponseEntity.ok().build();
		} catch(EntityNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}*/

	@PostMapping(value = "/create-from-payment-event", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Set<String>> createSubscriptionsFromPaymentEvent(@RequestBody PaymentMessage message) {
		return createSubscriptionsFromOrderId(message.getOrderId(), message.getUserId());
	}

	private void triggerSubscriptionCreatedEvent(Subscription subscription) {
		SubscriptionMessage subscriptionMessage = SubscriptionMessage.builder()
				.eventType(EventType.SUBSCRIPTION_CREATED)
				.namespace(subscription.getNamespace())
				.subscriptionId(subscription.getId())
				.timestamp(subscription.getCreatedAt().toString()) // TODO check format
				.build();
		sendEventService.sendEventMessage(TopicName.SUBSCRIPTIONS, subscriptionMessage);
		log.debug("triggered event SUBSCRIPTION_CREATED for subscriptionId: " + subscription.getId());
	}


}
