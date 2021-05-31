package fi.helshop.microservice.recurringorder.service.recurringorder;

import fi.helshop.microservice.shared.mapper.ObjectMapper;
import fi.helshop.microservice.shared.service.DefaultCreateEntityCommand;
import fi.helshop.microservice.recurringorder.model.recurringorder.RecurringOrder;
import fi.helshop.microservice.recurringorder.repository.jpa.RecurringOrderRepository;
import fi.helshop.microservice.recurringorder.rest.model.RecurringOrderDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

@Service
public class CreateRecurringOrderCommand extends DefaultCreateEntityCommand<RecurringOrder, RecurringOrderDto, String> {

	@Autowired
	public CreateRecurringOrderCommand(
			RecurringOrderRepository repository,
			ObjectMapper objectMapper,
			@Qualifier("beanValidator") Validator validator
	) {
		super(repository, objectMapper, validator, RecurringOrderDto.class);
	}

	// TODO: override necessary methods
}
