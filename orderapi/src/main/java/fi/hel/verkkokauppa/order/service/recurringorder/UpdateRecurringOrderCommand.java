package fi.hel.verkkokauppa.order.service.recurringorder;

import fi.hel.verkkokauppa.order.api.data.recurringorder.RecurringOrderDto;
import fi.hel.verkkokauppa.order.model.recurringorder.RecurringOrder;
import fi.hel.verkkokauppa.shared.mapper.ObjectMapper;
import fi.hel.verkkokauppa.shared.repository.jpa.BaseRepository;
import fi.hel.verkkokauppa.shared.service.DefaultUpdateEntityCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import java.time.Instant;

@Service
public class UpdateRecurringOrderCommand extends DefaultUpdateEntityCommand<RecurringOrder, RecurringOrderDto, String> {

	@Autowired
	public UpdateRecurringOrderCommand(
			BaseRepository<RecurringOrder, String> repository,
			ObjectMapper objectMapper,
			@Qualifier("beanValidator") Validator validator) {
		super(repository, objectMapper, validator, RecurringOrderDto.class);
	}

	// TODO: validoinnit yms.

	@Override
	protected void beforeSave(RecurringOrderDto dto, RecurringOrder recurringOrder) {
		super.beforeSave(dto, recurringOrder);

		recurringOrder.setUpdatedAt(Instant.now());
	}
}
