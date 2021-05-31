package fi.helshop.microservice.recurringorder.rest;

import fi.helshop.microservice.recurringorder.constants.RecurringOrderUrlConstants;
import fi.helshop.microservice.recurringorder.rest.model.RecurringOrderDto;
import fi.helshop.microservice.recurringorder.service.recurringorder.CreateRecurringOrderCommand;
import fi.helshop.microservice.recurringorder.service.recurringorder.GetRecurringOrderQuery;
import fi.helshop.microservice.recurringorder.service.recurringorder.SearchRecurringOrdersQuery;
import fi.helshop.microservice.recurringorder.service.recurringorder.UpdateRecurringOrderCommand;
import fi.helshop.microservice.shared.exception.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import fi.helshop.microservice.shared.model.impl.IdWrapper;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(RecurringOrderUrlConstants.CONTRACT_API_ROOT)
public class RecurringOrderController {

	private final CreateRecurringOrderCommand createRecurringOrderCommand;
	private final UpdateRecurringOrderCommand updateRecurringOrderCommand;
	private final GetRecurringOrderQuery getRecurringOrderQuery;
	private final SearchRecurringOrdersQuery searchRecurringOrdersQuery;

	@Autowired
	public RecurringOrderController(
			CreateRecurringOrderCommand createRecurringOrderCommand,
			UpdateRecurringOrderCommand updateRecurringOrderCommand,
			GetRecurringOrderQuery getRecurringOrderQuery,
			SearchRecurringOrdersQuery searchRecurringOrdersQuery
	) {
		this.createRecurringOrderCommand = createRecurringOrderCommand;
		this.updateRecurringOrderCommand = updateRecurringOrderCommand;
		this.getRecurringOrderQuery = getRecurringOrderQuery;
		this.searchRecurringOrdersQuery = searchRecurringOrdersQuery;
	}

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<RecurringOrderDto> getRecurringOrder(@RequestParam(value = "id") String id) {
		try {
			final RecurringOrderDto recurringOrder = getRecurringOrderQuery.getOne(id);
			return ResponseEntity.ok(recurringOrder);
		} catch(EntityNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@GetMapping(value = "/search/date/{date}/active", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<RecurringOrderDto>> searchActiveForDateRange(
			@RequestBody @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		try {
			final List<RecurringOrderDto> recurringOrder = searchRecurringOrdersQuery.searchActiveForDateRange(date);

			return ResponseEntity.ok(recurringOrder);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<IdWrapper> createRecurringOrder(@RequestBody RecurringOrderDto dto) {
		final String id = createRecurringOrderCommand.create(dto);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new IdWrapper(id));
	}

	@PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> updateRecurringOrder(@PathVariable("id") String id, @RequestBody RecurringOrderDto dto) {
		try {
			updateRecurringOrderCommand.update(id, dto);
			return ResponseEntity.ok().build();
		} catch(EntityNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}
}
