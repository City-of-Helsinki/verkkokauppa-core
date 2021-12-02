package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.EncryptorUtil;
import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.shared.mapper.ObjectMapper;
import fi.hel.verkkokauppa.shared.service.DefaultGetEntityQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetSubscriptionQuery extends DefaultGetEntityQuery<Subscription, SubscriptionDto, String> {

	private Logger log = LoggerFactory.getLogger(GetSubscriptionQuery.class);

	@Value("${payment.card_token.encryption.password}")
	private String cardTokenEncryptionPassword;

	@Autowired
	public GetSubscriptionQuery(
			SubscriptionRepository repository,
			ObjectMapper objectMapper) {
		super(repository, objectMapper, SubscriptionDto.class);
	}

	@Override
	public SubscriptionDto mapToDto(Subscription entity) {
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

	public Subscription findByIdValidateByUser(String id, String userId) {
		if (StringUtils.isEmpty(id) || StringUtils.isEmpty(userId)) {
			log.error("unauthorized attempt to load subscription, subscriptionId or userId missing");
			Error error = new Error("subscription-not-found-from-backend", "subscription with id [" + id + "] and user id ["+ userId +"] not found from backend");
			throw new CommonApiException(HttpStatus.NOT_FOUND, error);
		}

		Optional<Subscription> subscription = this.getRepository().findById(id);

		if (subscription.isEmpty()) {
			Error error = new Error("subscription-not-found-from-backend", "subscription with id [" + id + "] and user id ["+ userId +"] not found from backend");
			throw new CommonApiException(HttpStatus.NOT_FOUND, error);
		}

		String subscriptionUserId = subscription.get().getUser();
		if (subscriptionUserId == null || !subscriptionUserId.equals(userId)) {
			log.error("unauthorized attempt to load subscription, userId does not match");
			Error error = new Error("subscription-not-found-from-backend", "subscription with id [" + id + "] and user id ["+ userId +"] not found from backend");
			throw new CommonApiException(HttpStatus.NOT_FOUND, error);
		}
		return subscription.get();
	}

	public SubscriptionDto getOneValidateByUser(String id, String userId) {
		Subscription subscription = this.findByIdValidateByUser(id, userId);
		return this.mapToDto(subscription);
	}
}
