package fi.hel.verkkokauppa.productmapping.repository.serviceConfiguration;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import fi.hel.verkkokauppa.productmapping.model.serviceConfiguration.ServiceConfiguration;

@Repository
public interface ServiceConfigurationRepository extends ElasticsearchRepository<ServiceConfiguration, String> {

    List<ServiceConfiguration> findByNamespaceAndRestricted(String namespace, Boolean restricted);
    List<ServiceConfiguration> findByNamespaceAndConfigurationKeyAndRestricted(String namespace, String configurationKey, Boolean restricted);

    List<ServiceConfiguration> findByNamespace(String namespace);
    List<ServiceConfiguration> findByNamespaceAndConfigurationKey(String namespace, String configurationKey);

}
