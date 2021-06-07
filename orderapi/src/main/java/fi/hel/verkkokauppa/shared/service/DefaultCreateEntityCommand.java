package fi.hel.verkkokauppa.shared.service;

import fi.hel.verkkokauppa.shared.mapper.ObjectMapper;
import fi.hel.verkkokauppa.shared.model.Identified;
import fi.hel.verkkokauppa.shared.repository.jpa.BaseRepository;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import fi.hel.verkkokauppa.shared.model.Identifiable;

public class DefaultCreateEntityCommand<E extends Identifiable, D, ID>
		extends BaseServiceOperation
		implements CreateEntityCommand<D, ID> {

	private final BaseRepository<E, ID> repository;
	private final ObjectMapper objectMapper;
	private final Class<D> dtoType;

	public DefaultCreateEntityCommand(BaseRepository<E, ID> repository, ObjectMapper objectMapper, Validator validator, Class<D> dtoType) {
		super(validator);

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
	@Transactional
	public ID create(D dto) {
		assertRequiredParameterPresent(dto);
		if (dto instanceof Identified) {
			assertParameterAnonymous((Identified) dto);
		}

		final Errors errors = createErrors(dto);
		validateBean(dto, errors);
		assertNoErrors(errors);

		validateCreateParameter(dto, errors);

		assertNoErrors(errors);

		final E entity = mapToEntity(dto);

		validateBeforeSave(dto, entity, errors);

		assertNoErrors(errors);

		beforeSave(dto, entity);

		final E savedEntity = repository.save(entity);

		afterSave(dto, savedEntity);

		String id = savedEntity.getId();
		if (dto instanceof Identifiable) {
			((Identifiable) dto).setId(id);
		}

		return (ID) id;
	}

	protected E mapToEntity(D dto) {
		return objectMapper.mapObject(dto, repository.getEntityType());
	}

	protected void validateCreateParameter(D dto, Errors errors) {
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
