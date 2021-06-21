package fi.hel.verkkokauppa.order.service.recurringorder;

import fi.hel.verkkokauppa.order.model.recurringorder.RecurringOrder;
import fi.hel.verkkokauppa.order.model.recurringorder.Status;
import fi.hel.verkkokauppa.order.repository.jpa.RecurringOrderRepository;
import fi.hel.verkkokauppa.shared.exception.EntityNotFoundException;
import fi.hel.verkkokauppa.shared.service.BaseServiceOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Optional;

@Service
public class CancelRecurringOrderCommand extends BaseServiceOperation {

	private final RecurringOrderRepository repository;

	@Autowired
	public CancelRecurringOrderCommand(
			RecurringOrderRepository repository,
			@Qualifier("beanValidator") Validator validator
	) {
		super(validator);
		this.repository = repository;
	}

	public void cancel(String id) {
		final RecurringOrder recurringOrder = initRecurringOrder(id);

		checkAbilityToCancelRecurringOrder(recurringOrder);
		doCancel(recurringOrder);
	}

	private RecurringOrder initRecurringOrder(String id) {
		assertRequiredParameterPresent(id, "id");

		final Optional<RecurringOrder> result = repository.findById(id);
		if (result.isEmpty()) {
			throw new EntityNotFoundException(repository.getEntityType(), id);
		}

		return result.get();
	}

	private void checkAbilityToCancelRecurringOrder(RecurringOrder recurringOrder) {
		final Errors errors = createErrors(recurringOrder.getId());

		// TODO: implement needed validation logic and call it here

		assertNoErrors(errors);
	}

	private void doCancel(RecurringOrder recurringOrder) {
		recurringOrder.setStatus(Status.CANCELLED); // TODO: implement status logic separately?

		repository.save(recurringOrder);
	}
}
