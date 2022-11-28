package fi.hel.verkkokauppa.common.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
public abstract class AbstractModelListMapper<Entity, Dto> {

    protected final ObjectMapper mapper;
    private final Entity genericModelTypeObject;
    private final Dto genericDtoTypeObject;

    public AbstractModelListMapper(
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
    public List<Entity> fromDto(List<Dto> dto) {
        try {
            return mapper.readValue(
                    mapper.writeValueAsString(dto),
                    mapper.getTypeFactory().constructCollectionType(List.class, genericModelTypeObject.getClass())
            );
        } catch (JsonProcessingException e) {
            log.info("fromDto class {} failed to convert to {} : error {}",
                    genericDtoTypeObject.getClass(),
                    genericModelTypeObject.getClass(),
                    e
                    );
            return null;
        }
    }

    /**
     * Convert the Model object type of {@code <Entity>} to a Dto object type of {@code <Dto>}.
     *
     * @param entity The entity object to be converted to Dto
     * @return A model object
     */
    public List<Dto> toDto(List<Entity> entity) {
        try {
            return mapper.readValue(
                    mapper.writeValueAsString(entity),
                    mapper.getTypeFactory().constructCollectionType(List.class, genericDtoTypeObject.getClass())
            );
        } catch (JsonProcessingException e) {
            log.info("toDto class {} failed to convert to {} : error {}",
                    genericModelTypeObject.getClass(),
                    genericDtoTypeObject.getClass(),
                    e
            );
            return null;
        }
    }


}

