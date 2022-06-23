package fi.hel.verkkokauppa.payment.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.function.Supplier;

public class AbstractModelMapper<T, D> {

    protected final ObjectMapper mapper;
    private final T genericModelTypeObject;
    private final D genericDtoTypeObject;

    public AbstractModelMapper(
            ObjectMapper mapper,
            Supplier<T> modelSupplier,
            Supplier<D> dtoSupplier
    ) {
        this.mapper = mapper;
        this.genericModelTypeObject = modelSupplier.get();
        this.genericDtoTypeObject = dtoSupplier.get();
    }

    /**
     * Convert the given DTO type of {@code <D>} to a Model object type of {@code <T>}.
     *
     * @param dto The DTO object to be converted to a model
     * @return <T> A model object type
     */
    public T fromDto(D dto) {
        return (T) mapper.convertValue(dto, genericModelTypeObject.getClass());
    }

    /**
     * Convert the Model object type of {@code <T>} to a Dto object type of {@code <D>}.
     *
     * @param entity The entity object to be converted to Dto
     * @return A model object
     */
    public D toDto(T entity) {
        return (D) mapper.convertValue(entity, genericDtoTypeObject.getClass());
    }

    public T updateFromDtoToModel(T entity, D dto) throws JsonProcessingException {
        return mapper.readerForUpdating(entity).readValue(mapper.writeValueAsString(dto));
    }

}
