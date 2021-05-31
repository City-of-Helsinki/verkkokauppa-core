package fi.helshop.microservice.shared.service;

import fi.helshop.microservice.shared.exception.EntityNotFoundException;
import fi.helshop.microservice.shared.mapper.ObjectMapper;
import fi.helshop.microservice.shared.model.Identifiable;
import fi.helshop.microservice.shared.model.impl.BaseIdentifiableDto;
import fi.helshop.microservice.shared.repository.jpa.BaseRepository;

import java.util.Optional;

public class DefaultGetEntityQuery<E extends Identifiable, D extends BaseIdentifiableDto, ID> extends BaseServiceOperation implements GetEntityQuery<D, ID> {

	private final BaseRepository<E, ID> repository;
	private final ObjectMapper objectMapper;
	private final Class<D> dtoType;

	public DefaultGetEntityQuery(BaseRepository<E, ID> repository, ObjectMapper objectMapper, Class<D> dtoType) {
		if (repository == null) {
			throw new IllegalArgumentException("Value of argument 'repository' may not be null");
		}

		this.repository = repository;
		this.objectMapper = objectMapper;
		this.dtoType = dtoType;
	}

	protected BaseRepository<E, ID> getRepository() {
		return repository;
	}

	protected ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	protected Class<D> getDtoType() {
		return dtoType;
	}

	@Override
	public D getOne(ID id) {
		assertRequiredParameterPresent(id);

		final Optional<E> result = repository.findById(id);
		if (result.isEmpty()) {
			throw new EntityNotFoundException(repository.getEntityType(), id);
		}

		E entity = result.get();

		return mapToDto(entity);
	}

	protected D mapToDto(E entity) {
		return objectMapper.mapObject(entity, dtoType);
	}
}
