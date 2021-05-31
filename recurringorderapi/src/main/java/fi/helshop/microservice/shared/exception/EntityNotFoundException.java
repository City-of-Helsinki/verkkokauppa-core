package fi.helshop.microservice.shared.exception;

public class EntityNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public EntityNotFoundException(Class<?> type, Object id) {
		super(String.format("%s was not found for parameter %s", type.getSimpleName(), id));
	}
}
