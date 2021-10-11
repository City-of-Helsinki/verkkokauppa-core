package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.Status;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.shared.exception.EntityNotFoundException;
import fi.hel.verkkokauppa.shared.service.BaseServiceOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Optional;

@Service
public class CancelSubscriptionCommand extends BaseServiceOperation {

	private final SubscriptionRepository repository;

	@Autowired
	public CancelSubscriptionCommand(
			SubscriptionRepository repository,
			@Qualifier("beanValidator") Validator validator
	) {
		super(validator);
		this.repository = repository;
	}

	public void cancel(String id) {
		final Subscription subscription = initSubscription(id);

		checkAbilityToCancelSubscription(subscription);
		doCancel(subscription);
	}

	private Subscription initSubscription(String id) {
		assertRequiredParameterPresent(id, "id");

		final Optional<Subscription> result = repository.findById(id);
		if (result.isEmpty()) {
			throw new EntityNotFoundException(repository.getEntityType(), id);
		}

		return result.get();
	}

	private void checkAbilityToCancelSubscription(Subscription subscription) {
		final Errors errors = createErrors(subscription.getId());

		// TODO: implement needed validation logic and call it here

		assertNoErrors(errors);
	}

	private void doCancel(Subscription subscription) {
		subscription.setStatus(Status.CANCELLED); // TODO: implement status logic separately?

		repository.save(subscription);
	}
}
