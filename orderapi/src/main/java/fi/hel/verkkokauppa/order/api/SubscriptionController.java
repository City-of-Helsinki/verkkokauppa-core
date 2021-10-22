package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionCriteria;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.constants.SubscriptionUrlConstants;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.PaymentCardInfoDto;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionStatus;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.service.subscription.*;
import fi.hel.verkkokauppa.shared.exception.EntityNotFoundException;
import fi.hel.verkkokauppa.shared.model.impl.IdWrapper;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(SubscriptionUrlConstants.SUBSCRIPTION_API_ROOT)
public class SubscriptionController {
	@Autowired
	private final Environment env;

	private Logger log = LoggerFactory.getLogger(SubscriptionController.class);

	@Autowired
	private OrderService orderService;

	private final CreateSubscriptionCommand createSubscriptionCommand;
	//private final UpdateSubscriptionOrderCommand updateSubscriptionOrderCommand;
	private final GetSubscriptionQuery getSubscriptionQuery;
	private final SearchSubscriptionQuery searchSubscriptionQuery;
	private final CreateSubscriptionsFromOrderCommand createSubscriptionsFromOrderCommand;
	private final CancelSubscriptionCommand cancelSubscriptionCommand;
	private final UpdateSubscriptionCommand updateSubscriptionCommand;

	@Autowired
	public SubscriptionController(
			Environment env, CreateSubscriptionCommand createSubscriptionCommand,
			//UpdateSubscriptionOrderCommand updateSubscriptionOrderCommand,
			GetSubscriptionQuery getSubscriptionQuery,
			SearchSubscriptionQuery searchSubscriptionQuery,
			CreateSubscriptionsFromOrderCommand createSubscriptionsFromOrderCommand,
			CancelSubscriptionCommand cancelSubscriptionCommand,
			UpdateSubscriptionCommand updateSubscriptionCommand) {
		this.env = env;
		this.createSubscriptionCommand = createSubscriptionCommand;
		this.createSubscriptionsFromOrderCommand = createSubscriptionsFromOrderCommand;
		//this.updateSubscriptionOrderCommand = updateSubscriptionOrderCommand;
		this.getSubscriptionQuery = getSubscriptionQuery;
		this.searchSubscriptionQuery = searchSubscriptionQuery;
		this.cancelSubscriptionCommand = cancelSubscriptionCommand;
		this.updateSubscriptionCommand = updateSubscriptionCommand;
	}

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<SubscriptionDto> getSubscription(@RequestParam(value = "id") String id) {
		try {
			final SubscriptionDto subscription = getSubscriptionQuery.getOne(id);
			return ResponseEntity.ok(subscription);
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
	public ResponseEntity<Set<String>> createSubscriptionsFromOrder(@RequestBody OrderAggregateDto dto) {
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
		Order foundOrder = orderService.findByIdValidateByUser(orderId, userId);
		try {
			OrderAggregateDto dto = orderService.getOrderWithItems(foundOrder.getOrderId());
			Set<String> idList = createSubscriptionsFromOrderCommand.createFromOrder(dto);

			return ResponseEntity.status(HttpStatus.CREATED)
					.body(idList);
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
	public ResponseEntity<String> getSubscriptionCardToken(@RequestParam(value = "id") String id) {
		try {
			SubscriptionDto subscription = getSubscriptionQuery.getOne(id);
			String token = subscription.getPaymentMethodToken();

			return ResponseEntity.ok().body(token);
		} catch (Exception e) {
			log.error("getting payment method token from subscription with id [" + id + "] failed", e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-get-payment-method-token-from-subscription",
							"getting payment method token from subscription with id [" + id + "] failed")
			);
		}
	}

	@PutMapping(value = "/{id}/set-card-token", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> setSubscriptionCardToken(@PathVariable("id") String id, @RequestBody PaymentCardInfoDto dto) {
		try {
			String password = env.getRequiredProperty("payment.card_token.encryption.password");

			StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
			encryptor.setPassword(password);
			String encryptedToken = encryptor.encrypt(dto.getCardToken());

			SubscriptionDto subscriptionDto = getSubscriptionQuery.getOne(id);
			subscriptionDto.setPaymentMethodToken(encryptedToken);
			subscriptionDto.setPaymentMethodExpirationYear(dto.getExpYear());
			subscriptionDto.setPaymentMethodExpirationMonth(dto.getExpMonth());
			updateSubscriptionCommand.update(id, subscriptionDto);

			return ResponseEntity.ok().build();
		} catch (Exception e) {
			log.error("setting payment method token for subscription with id [" + id + "] failed", e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-set-payment-method-token-for-subscription",
							"setting payment method token for subscription with id [" + id + "] failed")
			);
		}
	}

	@PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> cancelSubscription(@PathVariable("id") String id) {
		try {
			cancelSubscriptionCommand.cancel(id);
			return ResponseEntity.ok().build();
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
}
