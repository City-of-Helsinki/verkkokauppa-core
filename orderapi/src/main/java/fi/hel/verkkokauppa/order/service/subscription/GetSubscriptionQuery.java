package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.common.util.EncryptorUtil;
import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.shared.mapper.ObjectMapper;
import fi.hel.verkkokauppa.shared.service.DefaultGetEntityQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GetSubscriptionQuery extends DefaultGetEntityQuery<Subscription, SubscriptionDto, String> {

	@Value("${payment.card_token.encryption.password}")
	private String cardTokenEncryptionPassword;

	@Autowired
	public GetSubscriptionQuery(
			SubscriptionRepository repository,
			ObjectMapper objectMapper) {
		super(repository, objectMapper, SubscriptionDto.class);
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
			String decrypt = EncryptorUtil.decryptValue(encryptedToken, cardTokenEncryptionPassword);
			dto.setPaymentMethodToken(decrypt);
		}
	}

}
