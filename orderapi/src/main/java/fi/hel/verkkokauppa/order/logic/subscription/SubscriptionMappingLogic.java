package fi.hel.verkkokauppa.order.logic.subscription;

import fi.hel.verkkokauppa.order.api.data.subscription.MerchantDto;
import fi.hel.verkkokauppa.order.api.data.subscription.ProductDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMappingLogic {

	public void mapMerchantDataToDto(Subscription entity, SubscriptionDto dto) {
		MerchantDto merchant = new MerchantDto();
		merchant.setName(entity.getMerchantName());
		merchant.setNamespace(entity.getMerchantNamespace());

		dto.setMerchant(merchant);
	}

	public void mapProductDataToDto(Subscription entity, SubscriptionDto dto) {
		ProductDto product = new ProductDto();
		product.setId(entity.getProductId());
		product.setName(entity.getProductName());

		dto.setProduct(product);
	}

	public void mapMerchantDataToEntity(SubscriptionDto dto, Subscription entity) {
		entity.setMerchantName(dto.getMerchant().getName());
		entity.setMerchantNamespace(dto.getMerchant().getNamespace());
	}

	public void mapProductDataToEntity(SubscriptionDto dto, Subscription entity) {
		entity.setProductId(dto.getProduct().getId());
		entity.setProductName(dto.getProduct().getName());
	}

}
