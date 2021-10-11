package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.logic.subscription.SubscriptionMappingLogic;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.shared.mapper.ObjectMapper;
import fi.hel.verkkokauppa.shared.service.DefaultGetEntityQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GetSubscriptionQuery extends DefaultGetEntityQuery<Subscription, SubscriptionDto, String> {

	private final SubscriptionMappingLogic subscriptionMappingLogic;

	@Autowired
	public GetSubscriptionQuery(
			SubscriptionRepository repository,
			ObjectMapper objectMapper,
			SubscriptionMappingLogic subscriptionMappingLogic
	) {
		super(repository, objectMapper, SubscriptionDto.class);
		this.subscriptionMappingLogic = subscriptionMappingLogic;
	}

	@Override
	protected SubscriptionDto mapToDto(Subscription entity) {
		final SubscriptionDto dto = super.mapToDto(entity);

		subscriptionMappingLogic.mapMerchantDataToDto(entity, dto);
		subscriptionMappingLogic.mapProductDataToDto(entity, dto);
		//SubscriptionMappingLogic.mapBillingAddressDataToDto(entity, dto); // TODO?
		//SubscriptionMappingLogic.mapShippingAddressDataToDto(entity, dto); // TODO?

		return dto;
	}
}
