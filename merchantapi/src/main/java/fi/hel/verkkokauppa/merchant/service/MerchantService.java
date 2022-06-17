package fi.hel.verkkokauppa.merchant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.merchant.api.dto.MerchantDto;
import fi.hel.verkkokauppa.merchant.mapper.MerchantMapper;
import fi.hel.verkkokauppa.merchant.model.MerchantModel;
import fi.hel.verkkokauppa.merchant.repository.MerchantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class MerchantService {
    @Autowired
    private MerchantMapper mapper;
    @Autowired
    private MerchantRepository merchantRepository;

    /**
     * Save the merchant and return the saved merchant as a DTO.
     *
     * @param dto The DTO object that will be saved to the database.
     * @return A MerchantDto object
     */
    public MerchantDto save(MerchantDto dto) {
        MerchantModel entity = mapper.fromDto(dto);
        entity.setMerchantId(UUIDGenerator.generateType4UUID().toString());
        entity.setCreatedAt(DateTimeUtil.getFormattedDateTime());
        MerchantModel saved = merchantRepository.save(entity);
        return mapper.toDto(saved);
    }


    /**
     * > Update a merchant entity from a DTO, and return the updated DTO
     *
     * @param dto The DTO object that contains the data to be updated.
     * @return MerchantDto
     */
    public MerchantDto update(MerchantDto dto) throws CommonApiException {
        MerchantModel entity = merchantRepository
                .findById(dto.getMerchantId())
                .orElseThrow(() -> new CommonApiException(
                        HttpStatus.NOT_FOUND,
                        new Error("merchant-not-found", "merchant with id [" + dto.getMerchantId() + "] not found")
                ));

        MerchantModel updatedEntity;
        try {
            updatedEntity = mapper.updateFromDtoToModel(entity, dto);
            updatedEntity.setUpdatedAt(DateTimeUtil.getFormattedDateTime());
        } catch (JsonProcessingException e) {
            throw new CommonApiException(
                    HttpStatus.NOT_FOUND,
                    new Error("merchant-dto-json-error", "merchant with id [" + dto.getMerchantId() + "] json processing error")
            );
        }

        return mapper.toDto(merchantRepository.save(updatedEntity));
    }

}
