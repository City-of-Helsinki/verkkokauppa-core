package fi.hel.verkkokauppa.common.elastic.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.mapper.AbstractModelMapper;

import java.util.function.Supplier;


public class SearchResultMapper<Entity, Dto> extends AbstractModelMapper<Entity, Dto> {

    public SearchResultMapper(ObjectMapper mapper, Supplier<Entity> modelSupplier, Supplier<Dto> dtoSupplier) {
        super(mapper, modelSupplier, dtoSupplier);
    }
}
