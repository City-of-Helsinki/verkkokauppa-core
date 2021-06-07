package fi.hel.verkkokauppa.shared.service;

public interface UpdateEntityCommand<T, ID> {

	public void update(ID id, T dto);
}
