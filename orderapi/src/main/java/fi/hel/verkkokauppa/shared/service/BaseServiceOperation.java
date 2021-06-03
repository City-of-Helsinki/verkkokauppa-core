package fi.hel.verkkokauppa.shared.service;

import fi.hel.verkkokauppa.shared.error.SingleGlobalError;
import fi.hel.verkkokauppa.shared.exception.InvalidServiceCallInputException;
import fi.hel.verkkokauppa.shared.model.Identified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public abstract class BaseServiceOperation {

	private static final Logger logger = LoggerFactory.getLogger(BaseServiceOperation.class);

	private final Validator validator;

	protected BaseServiceOperation() {
		this(null);
	}

	protected BaseServiceOperation(Validator validator) {
		this.validator = validator;
	}

	protected Validator getValidator() {
		return validator;
	}

	protected void assertRequiredParameterPresent(Object parameter) {
		if (parameter == null) {
			final String errorMessage = "Required parameter must not be null";
			final Errors errors = new SingleGlobalError("parameter", "error.parameter.required", new String[0], "error.parameter.required");
			throw new InvalidServiceCallInputException(errorMessage, errors);
		}
	}

	protected void assertRequiredParameterPresent(Object parameter, String name) {
		if (parameter == null) {
			final String errorMessage = "Required parameter [" + name + "] must not be null";
			final Errors errors = new SingleGlobalError(name, "error.parameter.required-named", new String[] {name}, "error.parameter.required-named");
			throw new InvalidServiceCallInputException(errorMessage, errors);
		}
	}

	/**
	 * Throws {@link InvalidServiceCallInputException} if this parameter is null or empty.
	 */
	protected void assertRequiredParameterNotEmpty(String parameter) {
		if (parameter == null || parameter.isEmpty()) {
			final String errorMessage = "Required parameter must not be null or empty";
			final Errors errors = new SingleGlobalError("parameter", "error.parameter.required-not-empty", new String[0], "error.parameter.required.not-empty");
			throw new InvalidServiceCallInputException(errorMessage, errors);
		}
	}

	protected void assertRequiredParameterNotEmpty(String parameter, String name) {
		if (parameter == null || parameter.isEmpty()) {
			final String errorMessage = "Required parameter [" + name + "] must not be null or empty";
			final Errors errors = new SingleGlobalError(name, "error.parameter.required-not-empty-named", new String[] {name}, "error.parameter.required-not-empty-named");
			throw new InvalidServiceCallInputException(errorMessage, errors);
		}
	}

	/**
	 * Throws {@link InvalidServiceCallInputException} if the given object already has an id.
	 */
	protected void assertParameterAnonymous(Identified parameter) {
		if (parameter.getId() != null) {
			final String errorMessage = "Parameter must not have an id defined";
			final Errors errors = new SingleGlobalError("parameter", "error.parameter.anonymous-required", new String[0], "error.parameter.anonymous-required");
			throw new InvalidServiceCallInputException(errorMessage, errors);
		}
	}

	/**
	 * Throws {@link InvalidServiceCallInputException} if the given object has no id.
	 */
	protected void assertParameterNotAnonymous(Identified parameter) {
		if (parameter.getId() == null) {
			final String errorMessage = "Parameter must have an id defined";
			final Errors errors = new SingleGlobalError("parameter", "error.parameter.anonymous", new String[0], "error.parameter.anonymous");
			throw new InvalidServiceCallInputException(errorMessage, errors);
		}
	}

	protected BeanPropertyBindingResult createErrors(Object object) {
		return new BeanPropertyBindingResult(object, "parameter"); // TODO: ok?
	}

	/**
	 * Throws {@link InvalidServiceCallInputException} if the given object is not valid as per bean validation.
	 */
	protected void assertParameterValid(Object parameter) {
		if (parameter == null) {
			// not concerned with the parameter being required
			return;
		}

		final Errors errors = createErrors(parameter);

		validateBean(parameter, errors);
		assertNoErrors(errors);
	}

	/**
	 * Validates the object as per bean validation.
	 */
	protected void validateBean(Object object, Errors errors) {
		if (validator == null) {
			// validation not active (e.g. tests)
			return;
		}

		validator.validate(object, errors);
	}

	/**
	 * Throws {@link InvalidServiceCallInputException} if there are any errors.
	 */
	protected void assertNoErrors(Errors errors) {
		if (errors.hasErrors()) {
			logger.debug("Validation errors {}", errors.getAllErrors());
			raiseValidationException(errors);
		}
	}

	protected void raiseValidationException(Errors errors) {
		final String errorMessage = "Parameter [" + errors.getObjectName() + "] is invalid";
		if (errors.hasFieldErrors()) {
			errors.reject("error.parameter.invalid", new String[]{errors.getObjectName()}, "error.parameter.invalid");
		}
		throw new InvalidServiceCallInputException(errorMessage, errors);
	}
}
