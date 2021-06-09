package fi.hel.verkkokauppa.productmapping.service;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import fi.hel.verkkokauppa.productmapping.model.ServiceMapping;

@Repository
public interface ServiceMappingRepository extends ElasticsearchRepository<ServiceMapping, String> {      

    List<ServiceMapping> findByNamespace(String namespace);
    List<ServiceMapping> findByNamespaceAndType(String namespace, String type);

}
