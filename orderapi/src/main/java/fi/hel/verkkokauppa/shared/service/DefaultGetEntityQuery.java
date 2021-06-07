package fi.hel.verkkokauppa.shared.service;

import fi.hel.verkkokauppa.shared.exception.EntityNotFoundException;
import fi.hel.verkkokauppa.shared.mapper.ObjectMapper;
import fi.hel.verkkokauppa.shared.model.Identifiable;
import fi.hel.verkkokauppa.shared.model.impl.BaseIdentifiableDto;
import fi.hel.verkkokauppa.shared.repository.jpa.BaseRepository;
import org.springframework.transaction.annotation.Transactional;

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
	@Transactional(readOnly = true)
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
