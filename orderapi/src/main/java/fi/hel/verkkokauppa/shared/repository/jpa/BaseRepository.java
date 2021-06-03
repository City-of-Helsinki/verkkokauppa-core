package fi.hel.verkkokauppa.shared.repository.jpa;

import fi.hel.verkkokauppa.shared.model.Identifiable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseRepository<T extends Identifiable, ID> extends ElasticsearchRepository<T, ID> {

	Class<T> getEntityType();
}