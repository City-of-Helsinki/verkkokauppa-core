package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.logic.subscription.SubscriptionMappingLogic;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.shared.mapper.ObjectMapper;
import fi.hel.verkkokauppa.shared.service.DefaultGetEntityQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class GetSubscriptionQuery extends DefaultGetEntityQuery<Subscription, SubscriptionDto, String> {

	@Autowired
	public GetSubscriptionQuery(
			SubscriptionRepository repository,
			ObjectMapper objectMapper,
			Environment env) {
		super(repository, objectMapper, SubscriptionDto.class);
	}

}
