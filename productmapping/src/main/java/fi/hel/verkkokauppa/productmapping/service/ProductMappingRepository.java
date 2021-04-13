package fi.hel.verkkokauppa.productmapping.service;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.hel.verkkokauppa.productmapping.model.ProductMapping;

@Repository
public interface ProductMappingRepository
  extends CrudRepository<ProductMapping, String> {
}
