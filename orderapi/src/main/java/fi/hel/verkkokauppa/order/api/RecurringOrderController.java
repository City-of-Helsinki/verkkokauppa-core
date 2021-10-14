package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.recurringorder.RecurringOrderCriteria;
import fi.hel.verkkokauppa.order.api.data.recurringorder.RecurringOrderDto;
import fi.hel.verkkokauppa.order.constants.RecurringOrderUrlConstants;
import fi.hel.verkkokauppa.order.model.recurringorder.Status;
import fi.hel.verkkokauppa.order.service.recurringorder.CancelRecurringOrderCommand;
import fi.hel.verkkokauppa.order.service.recurringorder.CreateRecurringOrderCommand;
import fi.hel.verkkokauppa.order.service.recurringorder.CreateRecurringOrdersFromOrderCommand;
import fi.hel.verkkokauppa.order.service.recurringorder.GetRecurringOrderQuery;
import fi.hel.verkkokauppa.order.service.recurringorder.SearchRecurringOrdersQuery;
import fi.hel.verkkokauppa.order.service.recurringorder.UpdateRecurringOrderCommand;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(RecurringOrderUrlConstants.RECURRING_ORDER_API_ROOT)
public class RecurringOrderController {

	private Logger log = LoggerFactory.getLogger(RecurringOrderController.class);

	private final CreateRecurringOrderCommand createRecurringOrderCommand;
	private final UpdateRecurringOrderCommand updateRecurringOrderCommand;
	private final GetRecurringOrderQuery getRecurringOrderQuery;
	private final SearchRecurringOrdersQuery searchRecurringOrdersQuery;
	private final CreateRecurringOrdersFromOrderCommand createRecurringOrdersFromOrderCommand;
	private final CancelRecurringOrderCommand cancelRecurringOrderCommand;
	private final Environment env;

	@Autowired
	public RecurringOrderController(
			CreateRecurringOrderCommand createRecurringOrderCommand,
			UpdateRecurringOrderCommand updateRecurringOrderCommand,
			GetRecurringOrderQuery getRecurringOrderQuery,
			SearchRecurringOrdersQuery searchRecurringOrdersQuery,
			CreateRecurringOrdersFromOrderCommand createRecurringOrdersFromOrderCommand,
			CancelRecurringOrderCommand cancelRecurringOrderCommand,
			Environment env
	) {
		this.createRecurringOrderCommand = createRecurringOrderCommand;
		this.createRecurringOrdersFromOrderCommand = createRecurringOrdersFromOrderCommand;
		this.updateRecurringOrderCommand = updateRecurringOrderCommand;
		this.getRecurringOrderQuery = getRecurringOrderQuery;
		this.searchRecurringOrdersQuery = searchRecurringOrdersQuery;
		this.cancelRecurringOrderCommand = cancelRecurringOrderCommand;
		this.env = env;
	}

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<RecurringOrderDto> getRecurringOrder(@RequestParam(value = "id") String id) {
		try {
			final RecurringOrderDto recurringOrder = getRecurringOrderQuery.getOne(id);
			return ResponseEntity.ok(recurringOrder);
		} catch(EntityNotFoundException e) {
			log.error("Exception on getting recurring order", e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@PostMapping(value = "/search/active", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<RecurringOrderDto>> searchActive(
			@RequestBody RecurringOrderCriteria criteria
	) {
		try {
			// It's possible to give other status in criteria, but we always want to search for active recurring order.
			criteria.setStatus(Status.ACTIVE);

			final List<RecurringOrderDto> recurringOrder = searchRecurringOrdersQuery.searchActive(criteria);
			return ResponseEntity.ok(recurringOrder);
		} catch (Exception e) {
			log.error("Exception on searching recurring order", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<IdWrapper> createRecurringOrder(@RequestBody RecurringOrderDto dto) {
		final String id = createRecurringOrderCommand.create(dto);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new IdWrapper(id));
	}

	@PostMapping(value = "/create-from-order", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Set<String>> createRecurringOrdersFromOrder(@RequestBody OrderAggregateDto dto) {
		try {
			Set<String> idList = createRecurringOrdersFromOrderCommand.createFromOrder(dto);

			return ResponseEntity.status(HttpStatus.CREATED)
					.body(idList);
		} catch (Exception e) {
			log.error("Exception on creating recurring order from order", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping(value = "/get-card-token", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getRecurringOrderCardToken(@RequestParam(value = "id") String id) {
		try {
			RecurringOrderDto recurringOrder = getRecurringOrderQuery.getOne(id);
			String token = recurringOrder.getPaymentMethodToken();

			return ResponseEntity.ok().body(token);
		} catch (Exception e) {
			log.error("getting payment method token from recurring order with id [" + id + "] failed", e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-get-payment-method-token-from-recurring-order",
							"getting payment method token from recurring order with id [" + id + "] failed")
			);
		}
	}

	@PutMapping(value = "/set-card-token", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> setRecurringOrderCardToken(@RequestParam(value = "id") String id, @RequestParam("token") String token) {
		try {
			String password = env.getRequiredProperty("payment.card_token.encryption.password");

			StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
			encryptor.setPassword(password);
			String encryptedToken = encryptor.encrypt(token);

			RecurringOrderDto recurringOrder = getRecurringOrderQuery.getOne(id);
			recurringOrder.setPaymentMethodToken(encryptedToken);
			updateRecurringOrderCommand.update(id, recurringOrder);

			return ResponseEntity.ok().build();
		} catch (Exception e) {
			log.error("setting payment method token for recurring order with id [" + id + "] failed", e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-set-payment-method-token-for-recurring-order",
							"setting payment method token for recurring order with id [" + id + "] failed")
			);
		}
	}

	@PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> cancelRecurringOrder(@PathVariable("id") String id) {
		try {
			cancelRecurringOrderCommand.cancel(id);
			return ResponseEntity.ok().build();
		} catch(EntityNotFoundException e) {
			log.error("Exception on cancelling recurring order", e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	/*@PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> updateRecurringOrder(@PathVariable("id") String id, @RequestBody RecurringOrderDto dto) {
		try {
			updateRecurringOrderCommand.update(id, dto);
			return ResponseEntity.ok().build();
		} catch(EntityNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}*/
}
