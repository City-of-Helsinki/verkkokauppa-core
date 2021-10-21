package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionCriteria;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.logic.subscription.SubscriptionMappingLogic;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionQueryBuilderBuilder;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.shared.mapper.ListMapper;
import fi.hel.verkkokauppa.shared.service.DefaultSearchEntitiesQuery;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchSubscriptionQuery extends
		DefaultSearchEntitiesQuery<Subscription, SubscriptionDto, SubscriptionCriteria, String> {

	private final SubscriptionMappingLogic subscriptionMappingLogic;

	@Autowired
	public SearchSubscriptionQuery(
			SubscriptionRepository repository,
			SubscriptionQueryBuilderBuilder subscriptionQueryBuilderBuilder,
			ListMapper listMapper,
			SubscriptionMappingLogic subscriptionMappingLogic
	) {
		super(repository, subscriptionQueryBuilderBuilder, listMapper, SubscriptionDto.class);
		this.subscriptionMappingLogic = subscriptionMappingLogic;
	}

	public List<SubscriptionDto> searchActive(SubscriptionCriteria criteria) {
		return super.search(criteria);
	}

	protected void mapItemToDto(Subscription entity, SubscriptionDto dto) {
//		subscriptionMappingLogic.mapUserDataToDto(entity, dto);
//		subscriptionMappingLogic.mapProductDataToDto(entity, dto);
		ModelMapper mm = new ModelMapper();
		mm.map(entity,dto);
		//SubscriptionMappingLogic.mapBillingAddressDataToDto(entity, dto); // TODO?
		//SubscriptionMappingLogic.mapShippingAddressDataToDto(entity, dto); // TODO?
	}
}
