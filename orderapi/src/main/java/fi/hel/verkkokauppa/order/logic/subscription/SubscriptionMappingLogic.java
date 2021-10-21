package fi.hel.verkkokauppa.order.logic.subscription;

import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMappingLogic {

	public void mapUserDataToEntity(SubscriptionDto dto, Subscription entity) {
		entity.setUser(dto.getUser());
		entity.setNamespace(dto.getNamespace());
	}

	public void mapCustomerDataToEntity(SubscriptionDto dto, Subscription entity) {
		entity.setCustomerPhone(dto.getCustomerPhone());
	}

	public void mapProductDataToEntity(SubscriptionDto dto, Subscription entity) {
		entity.setProductId(dto.getProductId());
		entity.setProductName(dto.getProductName());
	}

}
