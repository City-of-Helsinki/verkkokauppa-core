package fi.hel.verkkokauppa.shared.service;

import fi.hel.verkkokauppa.shared.model.impl.BaseIdentifiableDto;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface SearchEntitiesQuery<T extends BaseIdentifiableDto, C> {

	List<T> search(C criteria);
}
