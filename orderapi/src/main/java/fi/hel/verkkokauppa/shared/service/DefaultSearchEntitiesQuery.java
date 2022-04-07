package fi.hel.verkkokauppa.shared.service;

import fi.hel.verkkokauppa.common.util.AssertUtils;
import fi.hel.verkkokauppa.common.util.IterableUtils;
import fi.hel.verkkokauppa.shared.mapper.ListMapper;
import fi.hel.verkkokauppa.shared.model.Identifiable;
import fi.hel.verkkokauppa.shared.model.impl.BaseIdentifiableDto;
import fi.hel.verkkokauppa.shared.repository.jpa.BaseRepository;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

public class DefaultSearchEntitiesQuery<E extends Identifiable, D extends BaseIdentifiableDto, C, ID, T> extends BaseServiceOperation implements SearchEntitiesQuery<D, C> {

	private final BaseRepository<E, ID> repository;
	private final QueryBuilderBuilder<C> queryBuilderBuilder;
	private final ListMapper listMapper;
	private final Class<D> dtoType;
	private final Class<T> entityType;

	@Autowired
	private ElasticsearchOperations operations;

	public DefaultSearchEntitiesQuery(BaseRepository<E, ID> repository, QueryBuilderBuilder<C> queryBuilderBuilder, ListMapper listMapper, Class<D> dtoType, Class<T> entityType) {
		AssertUtils.assertArgumentNotNull(repository, "repository");
		AssertUtils.assertArgumentNotNull(queryBuilderBuilder, "queryBuilderBuilder");

		this.repository = repository;
		this.queryBuilderBuilder = queryBuilderBuilder;
		this.listMapper = listMapper;
		this.dtoType = dtoType;
		this.entityType = entityType;
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

	protected Class<T> getEntityType() {
		return entityType;
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

		final Iterable<T> result;

		NativeSearchQuery query = new NativeSearchQueryBuilder()
				.withQuery(qb)
				.withPageable(PageRequest.of(0, 10000))
				.build();

		SearchHits<T> hits = operations.search(query, getEntityType());

		SearchPage<T> searchHits = SearchHitSupport.searchPageFor(hits, query.getPageable());

		final List<T> resultList2 = searchHits.stream()
				.map(SearchHit::getContent)
				.collect(Collectors.toList());

		return mapToDtoList(resultList2);
	}

	protected List<D> mapToDtoList(List<T> entities) {
		return listMapper.mapList(entities, dtoType, this::mapItemToDto);
	}

	protected void mapItemToDto(T entity, D dto) {
		// nothing by default
	}
}
