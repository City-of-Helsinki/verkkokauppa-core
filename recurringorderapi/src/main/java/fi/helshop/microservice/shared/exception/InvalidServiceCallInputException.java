package fi.helshop.microservice.shared.exception;

import org.springframework.validation.Errors;

public class InvalidServiceCallInputException extends RuntimeException {

	private static final long serialVersionUID = -8126723625757567108L;

	@SuppressWarnings("NonSerializableFieldInSerializableClass")
	private final Errors errors;

	public InvalidServiceCallInputException(String message, Errors errors) {
		super(message);
		this.errors = errors;
	}

	public Errors getErrors() {
		return errors;
	}
}
