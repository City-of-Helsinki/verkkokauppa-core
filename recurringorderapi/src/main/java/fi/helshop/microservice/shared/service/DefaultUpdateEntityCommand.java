package fi.helshop.microservice.shared.service;

import fi.helshop.microservice.shared.exception.EntityNotFoundException;
import fi.helshop.microservice.shared.mapper.ObjectMapper;
import fi.helshop.microservice.shared.model.Identifiable;
import fi.helshop.microservice.shared.repository.jpa.BaseRepository;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Optional;

public class DefaultUpdateEntityCommand<E extends Identifiable, D, ID>
		extends BaseServiceOperation
		implements UpdateEntityCommand<D, ID> {

	private final BaseRepository<E, ID> repository;
	private final ObjectMapper objectMapper;
	private final Class<D> dtoType;

	public DefaultUpdateEntityCommand(BaseRepository<E, ID> repository, ObjectMapper objectMapper, Validator validator, Class<D> dtoType) {
		super(validator);

		this.repository = repository;
		this.objectMapper = objectMapper;
		this.dtoType = dtoType;
	}

	protected ElasticsearchRepository<E, ID> getRepository() {
		return repository;
	}
	protected ObjectMapper getObjectMapper() {
		return objectMapper;
	}
	protected Class<D> getDtoType() {
		return dtoType;
	}

	@Override
	//@Transactional?
	public void update(ID id, D dto) {
		assertRequiredParameterPresent(id, "id");
		assertRequiredParameterPresent(dto, "dto");

		final Errors errors = createErrors(dto);
		validateBean(dto, errors);
		assertNoErrors(errors);

		final Optional<E> result = repository.findById(id);
		if (result.isEmpty()) {
			throw new EntityNotFoundException(repository.getEntityType(), id);
		}

		E entity = result.get();

		validateUpdateParameter(dto, entity, errors);

		assertNoErrors(errors);

		mapToEntity(dto, entity);

		validateBeforeSave(dto, entity, errors);

		assertNoErrors(errors);

		beforeSave(dto, entity);

		repository.save(entity);

		afterSave(dto, entity);
	}

	protected void mapToEntity(D dto, E entity) {
		objectMapper.mapObject(dto, entity);
	}

	protected void validateUpdateParameter(D dto, E entity, Errors errors) {
		// nothing by default
	}

	protected void validateBeforeSave(D dto, E entity, Errors errors) {
		// nothing by default
	}

	protected void beforeSave(D dto, E entity) {
		// nothing by default
	}

	protected void afterSave(D dto, E entity) {
		// nothing by default
	}
}
