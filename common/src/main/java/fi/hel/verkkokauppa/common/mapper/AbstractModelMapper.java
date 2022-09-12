package fi.hel.verkkokauppa.common.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.function.Supplier;

public abstract class AbstractModelMapper<Entity, Dto> {

    protected final ObjectMapper mapper;
    private final Entity genericModelTypeObject;
    private final Dto genericDtoTypeObject;

    public AbstractModelMapper(
            ObjectMapper mapper,
            Supplier<Entity> modelSupplier,
            Supplier<Dto> dtoSupplier
    ) {
        this.mapper = mapper;
        this.genericModelTypeObject = modelSupplier.get();
        this.genericDtoTypeObject = dtoSupplier.get();
    }

    /**
     * Convert the given DTO type of {@code <Dto>} to a Model object type of {@code <Entity>}.
     *
     * @param dto The DTO object to be converted to a model
     * @return <Entity> A model object type
     */
    public Entity fromDto(Dto dto) {
        return (Entity) mapper.convertValue(dto, genericModelTypeObject.getClass());
    }

    /**
     * Convert the Model object type of {@code <Entity>} to a Dto object type of {@code <Dto>}.
     *
     * @param entity The entity object to be converted to Dto
     * @return A model object
     */
    public Dto toDto(Entity entity) {
        return (Dto) mapper.convertValue(entity, genericDtoTypeObject.getClass());
    }

    public Entity updateFromDtoToModel(Entity entity, Dto dto) throws JsonProcessingException {
        return mapper.readerForUpdating(entity).readValue(mapper.writeValueAsString(dto));
    }

}

