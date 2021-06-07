package fi.hel.verkkokauppa.shared.service;

import fi.hel.verkkokauppa.shared.model.impl.BaseIdentifiableDto;

public interface GetEntityQuery<T extends BaseIdentifiableDto, ID> {

	public T getOne(ID id);
}
