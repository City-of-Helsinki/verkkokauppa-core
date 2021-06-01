package fi.hel.verkkokauppa.productmapping.service;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import fi.hel.verkkokauppa.productmapping.model.ServiceConfiguration;

@Repository
public interface ServiceConfigurationRepository extends ElasticsearchRepository<ServiceConfiguration, String> {

    List<ServiceConfiguration> findByNamespace(String namespace);
    List<ServiceConfiguration> findByNamespaceAndConfigurationKey(String namespace, String configurationKey);

}
