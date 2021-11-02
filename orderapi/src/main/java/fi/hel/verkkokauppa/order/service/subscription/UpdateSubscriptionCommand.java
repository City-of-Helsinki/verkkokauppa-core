package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.shared.mapper.ObjectMapper;
import fi.hel.verkkokauppa.shared.repository.jpa.BaseRepository;
import fi.hel.verkkokauppa.shared.service.DefaultUpdateEntityCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
public class UpdateSubscriptionCommand extends DefaultUpdateEntityCommand<Subscription, SubscriptionDto, String> {

	@Autowired
	public UpdateSubscriptionCommand(
			BaseRepository<Subscription, String> repository,
			ObjectMapper objectMapper,
			@Qualifier("beanValidator") Validator validator) {
		super(repository, objectMapper, validator, SubscriptionDto.class);
	}

	// TODO: validoinnit yms.

	@Override
	protected void beforeSave(SubscriptionDto dto, Subscription subscription) {
		super.beforeSave(dto, subscription);

		subscription.setUpdatedAt(LocalDateTime.now());
	}
}
