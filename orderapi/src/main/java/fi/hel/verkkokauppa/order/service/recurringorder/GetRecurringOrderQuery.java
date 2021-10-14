package fi.hel.verkkokauppa.order.service.recurringorder;

import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.order.api.data.recurringorder.RecurringOrderDto;
import fi.hel.verkkokauppa.order.logic.RecurringOrderMappingLogic;
import fi.hel.verkkokauppa.order.model.recurringorder.RecurringOrder;
import fi.hel.verkkokauppa.order.repository.jpa.RecurringOrderRepository;
import fi.hel.verkkokauppa.shared.mapper.ObjectMapper;
import fi.hel.verkkokauppa.shared.service.DefaultGetEntityQuery;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class GetRecurringOrderQuery extends DefaultGetEntityQuery<RecurringOrder, RecurringOrderDto, String> {

	private final RecurringOrderMappingLogic recurringOrderMappingLogic;
	private final Environment env;

	@Autowired
	public GetRecurringOrderQuery(
			RecurringOrderRepository repository,
			ObjectMapper objectMapper,
			RecurringOrderMappingLogic recurringOrderMappingLogic,
			Environment env
	) {
		super(repository, objectMapper, RecurringOrderDto.class);
		this.recurringOrderMappingLogic = recurringOrderMappingLogic;
		this.env = env;
	}

	@Override
	protected RecurringOrderDto mapToDto(RecurringOrder entity) {
		final RecurringOrderDto dto = super.mapToDto(entity);

		decryptPaymentMethodToken(dto);

		recurringOrderMappingLogic.mapMerchantDataToDto(entity, dto);
		recurringOrderMappingLogic.mapProductDataToDto(entity, dto);
		//recurringOrderMappingLogic.mapBillingAddressDataToDto(entity, dto); // TODO?
		//recurringOrderMappingLogic.mapShippingAddressDataToDto(entity, dto); // TODO?

		return dto;
	}

	private void decryptPaymentMethodToken(RecurringOrderDto dto) {
		String encryptedToken = dto.getPaymentMethodToken();

		if (!StringUtils.isEmpty(encryptedToken)) {
			String password = env.getRequiredProperty("payment.card_token.encryption.password");

			StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
			encryptor.setPassword(password);
			dto.setPaymentMethodToken(encryptor.decrypt(encryptedToken));
		}
	}
}
