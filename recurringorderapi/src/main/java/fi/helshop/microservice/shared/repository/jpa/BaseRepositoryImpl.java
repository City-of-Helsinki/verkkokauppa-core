package fi.helshop.microservice.shared.repository.jpa;

import fi.helshop.microservice.shared.model.Identifiable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchEntityInformation;
import org.springframework.data.elasticsearch.repository.support.SimpleElasticsearchRepository;
import org.springframework.data.repository.NoRepositoryBean;

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
}
