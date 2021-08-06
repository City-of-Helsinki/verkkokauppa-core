package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.recurringorder.RecurringOrderCriteria;
import fi.hel.verkkokauppa.order.constants.RecurringOrderUrlConstants;
import fi.hel.verkkokauppa.order.api.data.recurringorder.RecurringOrderDto;
import fi.hel.verkkokauppa.order.model.recurringorder.Status;
import fi.hel.verkkokauppa.order.service.recurringorder.*;
import fi.hel.verkkokauppa.shared.exception.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import fi.hel.verkkokauppa.shared.model.impl.IdWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(RecurringOrderUrlConstants.RECURRING_ORDER_API_ROOT)
public class RecurringOrderController {

	private Logger log = LoggerFactory.getLogger(RecurringOrderController.class);

	private final CreateRecurringOrderCommand createRecurringOrderCommand;
	//private final UpdateRecurringOrderCommand updateRecurringOrderCommand;
	private final GetRecurringOrderQuery getRecurringOrderQuery;
	private final SearchRecurringOrdersQuery searchRecurringOrdersQuery;
	private final CreateRecurringOrdersFromOrderCommand createRecurringOrdersFromOrderCommand;
	private final CancelRecurringOrderCommand cancelRecurringOrderCommand;

	@Autowired
	public RecurringOrderController(
			CreateRecurringOrderCommand createRecurringOrderCommand,
			//UpdateRecurringOrderCommand updateRecurringOrderCommand,
			GetRecurringOrderQuery getRecurringOrderQuery,
			SearchRecurringOrdersQuery searchRecurringOrdersQuery,
			CreateRecurringOrdersFromOrderCommand createRecurringOrdersFromOrderCommand,
			CancelRecurringOrderCommand cancelRecurringOrderCommand
	) {
		this.createRecurringOrderCommand = createRecurringOrderCommand;
		this.createRecurringOrdersFromOrderCommand = createRecurringOrdersFromOrderCommand;
		//this.updateRecurringOrderCommand = updateRecurringOrderCommand;
		this.getRecurringOrderQuery = getRecurringOrderQuery;
		this.searchRecurringOrdersQuery = searchRecurringOrdersQuery;
		this.cancelRecurringOrderCommand = cancelRecurringOrderCommand;
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
