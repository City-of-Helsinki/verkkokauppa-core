package fi.hel.verkkokauppa.shared.repository.jpa;

import fi.hel.verkkokauppa.shared.model.Identifiable;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchEntityInformation;
import org.springframework.data.elasticsearch.repository.support.SimpleElasticsearchRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collections;
import java.util.stream.Collectors;

@NoRepositoryBean
public class BaseRepositoryImpl<T extends Identifiable, ID> extends SimpleElasticsearchRepository<T, ID> implements BaseRepository<T, ID> {
	private final Class<T> entityType;

	public BaseRepositoryImpl(ElasticsearchEntityInformation<T, ID> metadata, ElasticsearchOperations operations) {
		super(metadata, operations);
		this.entityType = metadata.getJavaType();
	}

	@Override
	public Class<T> getEntityType() {
		return entityType;
	}

	public Iterable<T> search(QueryBuilder query) {
		NativeSearchQuery searchQuery = (new NativeSearchQueryBuilder()).withQuery(query).build();
		long count = (Long)this.execute((operations) -> {
			return operations.count(searchQuery, this.entityClass, this.getIndexCoordinates());
		});

		if (count != 0L) {
			searchQuery.setPageable(PageRequest.of(0, (int) count));
			SearchHits<T> searchHits = (SearchHits) this.execute((operations) -> {
				return operations.search(searchQuery, this.entityClass, this.getIndexCoordinates());
			});

			if (searchHits != null) {
				return searchHits.getSearchHits()
						.stream()
						.map(SearchHit::getContent)
						.collect(Collectors.toList());
			}
		}
		return Collections.emptyList();
	}

	private IndexCoordinates getIndexCoordinates() {
		return this.operations.getIndexCoordinatesFor(this.entityClass);
	}
}
