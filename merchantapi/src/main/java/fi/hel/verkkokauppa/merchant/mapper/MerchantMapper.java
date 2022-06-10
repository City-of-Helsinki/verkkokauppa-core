package fi.hel.verkkokauppa.merchant.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.merchant.api.dto.MerchantDto;
import fi.hel.verkkokauppa.merchant.model.MerchantModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * > This class is a Spring component that maps a Merchant object to a MerchantDTO object and back
 */
@Component
public class MerchantMapper {
    // Injecting the ObjectMapper bean into the class.
    @Autowired
    ObjectMapper objectMapper;


    /**
     * Convert the given DTO to a MerchantModel object.
     *
     * @param dto The DTO object to be converted to a model.
     * @return A MerchantModel object
     */
    public MerchantModel fromDto(MerchantDto dto) {
        return objectMapper.convertValue(dto, MerchantModel.class);
    }

    /**
     * Convert the MerchantModel object to a MerchantDto object.
     *
     * @param entity The entity object to be converted to Dto
     * @return A MerchantDto object
     */
    public MerchantDto toDto(MerchantModel entity) {
        return objectMapper.convertValue(entity, MerchantDto.class);
    }

    public MerchantModel updateFromDtoToModel(MerchantModel entity, MerchantDto dto) throws JsonProcessingException {
        return objectMapper.readerForUpdating(entity).readValue(objectMapper.writeValueAsString(dto));
    }

}
