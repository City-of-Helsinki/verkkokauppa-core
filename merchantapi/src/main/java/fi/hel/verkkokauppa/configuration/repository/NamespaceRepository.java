package fi.hel.verkkokauppa.configuration.repository;


import fi.hel.verkkokauppa.configuration.model.namespace.NamespaceModel;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NamespaceRepository extends ElasticsearchRepository<NamespaceModel, String> {
    Optional<NamespaceModel> findByNamespace(String namespace);

}
