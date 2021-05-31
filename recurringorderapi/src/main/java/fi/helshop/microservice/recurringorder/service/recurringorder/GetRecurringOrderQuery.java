package fi.helshop.microservice.recurringorder.service.recurringorder;

import fi.helshop.microservice.shared.mapper.ObjectMapper;
import fi.helshop.microservice.shared.service.DefaultGetEntityQuery;
import fi.helshop.microservice.recurringorder.model.recurringorder.RecurringOrder;
import fi.helshop.microservice.recurringorder.repository.jpa.RecurringOrderRepository;
import fi.helshop.microservice.recurringorder.rest.model.RecurringOrderDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GetRecurringOrderQuery extends DefaultGetEntityQuery<RecurringOrder, RecurringOrderDto, String> {

	@Autowired
	public GetRecurringOrderQuery(RecurringOrderRepository repository, ObjectMapper objectMapper) {
		super(repository, objectMapper, RecurringOrderDto.class);
	}

	// TODO: overrides if needed
}
