package fi.hel.verkkokauppa.product.service;

import fi.hel.verkkokauppa.product.dto.BaseDto;
import fi.hel.verkkokauppa.product.model.BaseModel;

public interface Transformer<T extends BaseDto, S extends BaseModel> {

    S transform(T src);
    T transform(S src);
}
