package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.logic.subscription.SubscriptionMappingLogic;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.shared.mapper.ObjectMapper;
import fi.hel.verkkokauppa.shared.service.DefaultGetEntityQuery;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class GetSubscriptionQuery extends DefaultGetEntityQuery<Subscription, SubscriptionDto, String> {

	private final SubscriptionMappingLogic subscriptionMappingLogic;

	@Autowired
	private final Environment env;

	@Autowired
	public GetSubscriptionQuery(
			SubscriptionRepository repository,
			ObjectMapper objectMapper,
			SubscriptionMappingLogic subscriptionMappingLogic,
			Environment env) {
		super(repository, objectMapper, SubscriptionDto.class);
		this.subscriptionMappingLogic = subscriptionMappingLogic;
		this.env = env;
	}

	@Override
	protected SubscriptionDto mapToDto(Subscription entity) {
		final SubscriptionDto dto = super.mapToDto(entity);
		decryptPaymentMethodToken(dto);
		return dto;
	}

	private void decryptPaymentMethodToken(SubscriptionDto dto) {
		String encryptedToken = dto.getPaymentMethodToken();

		if (!StringUtils.isEmpty(encryptedToken)) {
			String password = env.getRequiredProperty("payment.card_token.encryption.password");

			StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
			encryptor.setPassword(password);
			dto.setPaymentMethodToken(encryptor.decrypt(encryptedToken));
		}
	}

//	@Override
//	protected SubscriptionDto mapToDto(Subscription entity) {
//		return super.mapToDto(entity);
//	}
}
