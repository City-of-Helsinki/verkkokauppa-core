package fi.hel.verkkokauppa.configuration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.configuration.api.merchant.dto.MerchantDto;
import fi.hel.verkkokauppa.configuration.mapper.MerchantMapper;
import fi.hel.verkkokauppa.configuration.model.ConfigurationModel;
import fi.hel.verkkokauppa.configuration.model.merchant.MerchantModel;
import fi.hel.verkkokauppa.configuration.repository.MerchantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
     * @return A NamespaceDto object
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
     * @return NamespaceDto
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

    /**
     * > Find the merchant with the given merchantId and namespace, then find the configuration with the given key, and
     * return the value of that configuration
     *
     * @param merchantId The merchantId of the merchant you want to get the configuration value for.
     * @param namespace  The namespace of the configuration.
     * @param key        The key of the configuration value you want to retrieve.
     * @return A String of configuration value or null
     */
    public String getConfigurationValueByMerchantIdAndNamespaceAndKey(String merchantId, String namespace, String key) {
        MerchantModel model = merchantRepository.findByMerchantIdAndNamespace(merchantId, namespace);

        Optional<ConfigurationModel> configuration = getConfigurationWithKeyFromModel(key, model);

        return configuration.map(ConfigurationModel::getValue).orElse(null);
    }

    public Optional<ConfigurationModel> getConfigurationWithKeyFromModel(String key, MerchantModel model) {
        return model.getConfigurations()
                .stream()
                .filter(configurationModel -> Objects.equals(configurationModel.getKey(), key))
                .findFirst();
    }

    public List<MerchantDto> findAllByNamespace(String namespace) {
        return merchantRepository
                .findAllByNamespace(namespace)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
}
