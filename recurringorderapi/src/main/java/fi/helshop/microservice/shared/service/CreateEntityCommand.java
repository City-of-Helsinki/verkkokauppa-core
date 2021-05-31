package fi.helshop.microservice.shared.service;

public interface CreateEntityCommand<T, ID> {

	public ID create(T dto);
}
