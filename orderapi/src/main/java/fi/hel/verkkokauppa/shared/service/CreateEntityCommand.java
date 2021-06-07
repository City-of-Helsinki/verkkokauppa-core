package fi.hel.verkkokauppa.shared.service;

public interface CreateEntityCommand<T, ID> {

	public ID create(T dto);
}
