package fi.hel.verkkokauppa.shared.service;

import fi.hel.verkkokauppa.common.util.AssertUtils;
import fi.hel.verkkokauppa.common.util.IterableUtils;
import fi.hel.verkkokauppa.shared.mapper.ListMapper;
import fi.hel.verkkokauppa.shared.model.Identifiable;
import fi.hel.verkkokauppa.shared.model.impl.BaseIdentifiableDto;
import fi.hel.verkkokauppa.shared.repository.jpa.BaseRepository;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class DefaultSearchEntitiesQuery<E extends Identifiable, D extends BaseIdentifiableDto, C, ID> extends BaseServiceOperation implements SearchEntitiesQuery<D, C> {

	private final BaseRepository<E, ID> repository;
	private final QueryBuilderBuilder<C> queryBuilderBuilder;
	private final ListMapper listMapper;
	private final Class<D> dtoType;

	public DefaultSearchEntitiesQuery(BaseRepository<E, ID> repository, QueryBuilderBuilder<C> queryBuilderBuilder, ListMapper listMapper, Class<D> dtoType) {
		AssertUtils.assertArgumentNotNull(repository, "repository");
		AssertUtils.assertArgumentNotNull(queryBuilderBuilder, "queryBuilderBuilder");

		this.repository = repository;
		this.queryBuilderBuilder = queryBuilderBuilder;
		this.listMapper = listMapper;
		this.dtoType = dtoType;
	}

	protected BaseRepository<E, ID> getRepository() {
		return repository;
	}

	protected ListMapper getListMapper() {
		return listMapper;
	}

	protected QueryBuilderBuilder<C> getQueryBuilderBuilder() {
		return queryBuilderBuilder;
	}

	protected Class<D> getDtoType() {
		return dtoType;
	}

	@Override
	@Transactional(readOnly = true)
	public List<D> search(C criteria) {
		assertRequiredParameterPresent(criteria);
		assertParameterValid(criteria);

		return searchImpl(criteria);
	}

	protected List<D> searchImpl(C criteria) {
		final QueryBuilder qb = queryBuilderBuilder.toQueryBuilder(criteria);

		final Iterable<E> result;

		result = repository.search(qb); // TODO: find better way to implement this

		final List<E> resultList = IterableUtils.iterableToList(result);

		return mapToDtoList(resultList);
	}

	protected List<D> mapToDtoList(List<E> entities) {
		return listMapper.mapList(entities, dtoType, this::mapItemToDto);
	}

	protected void mapItemToDto(E entity, D dto) {
		// nothing by default
	}
}
