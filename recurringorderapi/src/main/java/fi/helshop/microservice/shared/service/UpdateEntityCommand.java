package fi.helshop.microservice.shared.service;

public interface UpdateEntityCommand<T, ID> {

	public void update(ID id, T dto);
}
