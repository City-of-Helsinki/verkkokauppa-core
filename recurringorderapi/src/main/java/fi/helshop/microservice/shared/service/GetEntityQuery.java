package fi.helshop.microservice.shared.service;

import fi.helshop.microservice.shared.model.impl.BaseIdentifiableDto;

public interface GetEntityQuery<T extends BaseIdentifiableDto, ID> {

	public T getOne(ID id);
}
