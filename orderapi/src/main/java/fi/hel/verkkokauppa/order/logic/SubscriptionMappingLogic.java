package fi.hel.verkkokauppa.order.logic;

import com.alibaba.fastjson.JSON;
import fi.hel.verkkokauppa.order.api.data.subscription.AddressDto;
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

	public void mapBillingAddressDataToDto(Subscription entity, SubscriptionDto dto) {
		// TODO: add checks
		AddressDto address = JSON.parseObject(entity.getBillingAddressData(), AddressDto.class);
		address.setId(entity.getBillingAddressId());
		dto.setBillingAddress(address);
	}

	public void mapMerchantDataToEntity(SubscriptionDto dto, Subscription entity) {
		entity.setMerchantName(dto.getMerchant().getName());
		entity.setMerchantNamespace(dto.getMerchant().getNamespace());
	}

	public void mapProductDataToEntity(SubscriptionDto dto, Subscription entity) {
		entity.setProductId(dto.getProduct().getId());
		entity.setProductName(dto.getProduct().getName());
	}

	public void mapBillingAddressDataToEntity(SubscriptionDto dto, Subscription entity) {
		if (dto.getBillingAddress() != null) {
			entity.setBillingAddressId(dto.getBillingAddress().getId());
			entity.setBillingAddressData(JSON.toJSONString(dto.getBillingAddress())); // TODO: ok?
		}
	}

}
