package fi.hel.verkkokauppa.order.logic;

import com.alibaba.fastjson.JSON;
import fi.hel.verkkokauppa.order.api.data.recurringorder.AddressDto;
import fi.hel.verkkokauppa.order.api.data.recurringorder.MerchantDto;
import fi.hel.verkkokauppa.order.api.data.recurringorder.ProductDto;
import fi.hel.verkkokauppa.order.api.data.recurringorder.RecurringOrderDto;
import fi.hel.verkkokauppa.order.model.recurringorder.RecurringOrder;
import org.springframework.stereotype.Component;

@Component
public class RecurringOrderMappingLogic {

	public void mapMerchantDataToDto(RecurringOrder entity, RecurringOrderDto dto) {
		MerchantDto merchant = new MerchantDto();
		merchant.setName(entity.getMerchantName());
		merchant.setNamespace(entity.getMerchantNamespace());

		dto.setMerchant(merchant);
	}

	public void mapProductDataToDto(RecurringOrder entity, RecurringOrderDto dto) {
		ProductDto product = new ProductDto();
		product.setId(entity.getProductId());
		product.setName(entity.getProductName());

		dto.setProduct(product);
	}

	public void mapBillingAddressDataToDto(RecurringOrder entity, RecurringOrderDto dto) {
		// TODO: add checks
		AddressDto address = JSON.parseObject(entity.getBillingAddressData(), AddressDto.class);
		address.setId(entity.getBillingAddressId());
		dto.setBillingAddress(address);
	}

	public void mapShippingAddressDataToDto(RecurringOrder entity, RecurringOrderDto dto) {
		// TODO: add checks
		AddressDto address = JSON.parseObject(entity.getShippingAddressData(), AddressDto.class);
		address.setId(entity.getShippingAddressId());
		dto.setShippingAddress(address);
	}
	
	public void mapMerchantDataToEntity(RecurringOrderDto dto, RecurringOrder entity) {
		entity.setMerchantName(dto.getMerchant().getName());
		entity.setMerchantNamespace(dto.getMerchant().getNamespace());
	}

	public void mapProductDataToEntity(RecurringOrderDto dto, RecurringOrder entity) {
		entity.setProductId(dto.getProduct().getId());
		entity.setProductName(dto.getProduct().getName());
	}

	public void mapBillingAddressDataToEntity(RecurringOrderDto dto, RecurringOrder entity) {
		if (dto.getBillingAddress() != null) {
			entity.setBillingAddressId(dto.getBillingAddress().getId());
			entity.setBillingAddressData(JSON.toJSONString(dto.getBillingAddress())); // TODO: ok?
		}
	}

	public void mapShippingAddressDataToEntity(RecurringOrderDto dto, RecurringOrder entity) {
		if (dto.getShippingAddress() != null) {
			entity.setShippingAddressId(dto.getShippingAddress().getId());
			entity.setShippingAddressData(JSON.toJSONString(dto.getShippingAddress())); // TODO: ok?
		}
	}
}
