package fi.hel.verkkokauppa.configuration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.configuration.api.merchant.dto.ConfigurationDto;
import fi.hel.verkkokauppa.configuration.api.merchant.dto.MerchantDto;
import fi.hel.verkkokauppa.configuration.api.merchant.dto.PaytrailMerchantMappingDto;
import fi.hel.verkkokauppa.configuration.mapper.ConfigurationMapper;
import fi.hel.verkkokauppa.configuration.mapper.MerchantMapper;
import fi.hel.verkkokauppa.configuration.mapper.PaytrailMerchantMappingMapper;
import fi.hel.verkkokauppa.configuration.model.ConfigurationModel;
import fi.hel.verkkokauppa.configuration.model.LocaleModel;
import fi.hel.verkkokauppa.configuration.model.merchant.MerchantModel;
import fi.hel.verkkokauppa.configuration.model.merchant.PaytrailMerchantMapping;
import fi.hel.verkkokauppa.configuration.repository.MerchantRepository;
import fi.hel.verkkokauppa.configuration.repository.PaytrailMerchantMappingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class MerchantService {
    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private PaytrailMerchantMappingMapper paytrailMerchantMappingMapper;

    @Autowired
    private ConfigurationMapper configurationMapper;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private PaytrailMerchantMappingRepository paytrailMerchantMappingRepository;

    @Autowired
    private Environment env;

    @Autowired
    private EncryptService encryptService;

    @Autowired
    private DecryptService decryptService;

    /**
     * Save the merchant and return the saved merchant as a DTO.
     *
     * @param dto The DTO object that will be saved to the database.
     * @return A NamespaceDto object
     */
    public MerchantDto save(MerchantDto dto) {
        MerchantModel entity = merchantMapper.fromDto(dto);
        entity.setMerchantId(UUIDGenerator.generateType4UUID().toString());
        entity.setCreatedAt(DateTimeUtil.getFormattedDateTime());
        MerchantModel saved = merchantRepository.save(entity);
        return merchantMapper.toDto(saved);
    }

    public String encryptSecret(String secret) {
        String salt = env.getRequiredProperty("merchant.secret.encryption.salt");
        return encryptService.encryptSecret(secret, salt);
    }

    public void encryptSecret(PaytrailMerchantMapping mapping) {
        mapping.setMerchantPaytrailSecret(encryptSecret(mapping.getMerchantPaytrailSecret()));
    }

    public String decryptSecret(String secret) {
        String salt = env.getRequiredProperty("merchant.secret.encryption.salt");
        return decryptService.decryptSecret(salt, secret);
    }

    public void decryptSecret(PaytrailMerchantMapping mapping) {
        mapping.setMerchantPaytrailSecret(decryptSecret(mapping.getMerchantPaytrailSecret()));
    }

    public PaytrailMerchantMappingDto save(PaytrailMerchantMappingDto dto) {
        PaytrailMerchantMapping entity = paytrailMerchantMappingMapper.fromDto(dto);
        encryptSecret(entity);
        entity.setId(UUIDGenerator.generateType3UUIDString(entity.getNamespace(), entity.getMerchantPaytrailMerchantId()));
        entity = paytrailMerchantMappingRepository.save(entity);
        decryptSecret(entity);
        return paytrailMerchantMappingMapper.toDto(entity);
    }

    public PaytrailMerchantMappingDto getPaytrailMerchantMappingByMerchantPaytrailMerchantIdAndNamespace(
        String merchantPaytrailMerchantId,
        String namespace
    ) {
        String id = UUIDGenerator.generateType3UUIDString(namespace, merchantPaytrailMerchantId);
        if (id == null) {
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-paytrail-merchant-mapping", "failed to generate id for mapping with namespace, merchantPaytrailMerchantId: [" + namespace + ", " + merchantPaytrailMerchantId + "]")
            );
        }
        PaytrailMerchantMapping mapping = paytrailMerchantMappingRepository
                .findById(id)
                .orElse(null);
        if (mapping == null) {
            return null;
        }
        decryptSecret(mapping);
        return paytrailMerchantMappingMapper.toDto(mapping);
    }


    /**
     * > Update a merchant entity from a DTO, and return the updated DTO
     *
     * @param dto The DTO object that contains the data to be updated.
     */
    public MerchantDto update(MerchantDto dto) throws CommonApiException {
        MerchantModel entity = getMerchantModelByMerchantId(dto.getMerchantId());

        MerchantModel updatedEntity;
        try {
            // Get original configurations before appending/updating
            ArrayList<ConfigurationModel> originalDtoConfigurations = dto.getConfigurations();
            // List of configurations that are added to model
            ArrayList<ConfigurationModel> updatedConfigurations = new ArrayList<>();
            // Model existing configurations
            ArrayList<ConfigurationModel> existingModelConfigurations = entity.getConfigurations();

            updateConfigurations(dto, entity, originalDtoConfigurations, updatedConfigurations, existingModelConfigurations);

            updatedEntity = merchantMapper.updateFromDtoToModel(entity, dto);

            updatedEntity.setUpdatedAt(DateTimeUtil.getFormattedDateTime());
        } catch (JsonProcessingException e) {
            throw new CommonApiException(
                    HttpStatus.NOT_FOUND,
                    new Error("merchant-dto-json-error", "merchant with value: [" + dto.getNamespace() + "] json processing error")
            );
        }

        return merchantMapper.toDto(merchantRepository.save(updatedEntity));
    }

    /**
     * Checking if the existing model configurations are empty. If not, it is iterating through the existing model
     * configurations and checking if the key is present in the dto configurations. If it is, it is adding the dto
     * configuration to the updated configurations. If not, it is adding the existing model configuration to the
     * updated configurations.
     */
    private void updateConfigurations(MerchantDto dto, MerchantModel entity, ArrayList<ConfigurationModel> originalDtoConfigurations, ArrayList<ConfigurationModel> updatedConfigurations, ArrayList<ConfigurationModel> existingModelConfigurations) {
        if (!existingModelConfigurations.isEmpty()) {
            entity.getConfigurations().forEach(entityConfiguration -> {
                Optional<ConfigurationModel> foundConfiguration = dto.getConfigurations().stream()
                        .filter(dtoConfiguration -> Objects.equals(dtoConfiguration.getKey(), entityConfiguration.getKey()))
                        .findFirst();

                if (foundConfiguration.isPresent()) {
                    if (foundConfiguration.get().getLocale() != null) {
                        foundConfiguration.ifPresent(configurationModel -> {
                            // Values defaults to found entity locale data to prevent overriding
                            String en = configurationModel.getLocale().getEn() != null ? configurationModel.getLocale().getEn() : entityConfiguration.getLocale().getEn();
                            String sv = configurationModel.getLocale().getSv() != null ? configurationModel.getLocale().getSv() : entityConfiguration.getLocale().getSv();
                            String fi = configurationModel.getLocale().getFi() != null ? configurationModel.getLocale().getFi() : entityConfiguration.getLocale().getFi();

                            foundConfiguration.get().getLocale().setEn(en);
                            foundConfiguration.get().getLocale().setSv(sv);
                            foundConfiguration.get().getLocale().setFi(fi);
                        });
                    }

                    updatedConfigurations.add(foundConfiguration.get());
                } else {
                    updatedConfigurations.add(entityConfiguration);
                }
            });

            originalDtoConfigurations.forEach(dtoConfiguration -> {
                Optional<ConfigurationModel> foundConfiguration = updatedConfigurations.stream()
                        .filter(updatedConfiguration -> Objects.equals(updatedConfiguration.getKey(), dtoConfiguration.getKey()))
                        .findFirst();
                if (foundConfiguration.isEmpty()) {
                    updatedConfigurations.add(dtoConfiguration);
                }
            });

            dto.setConfigurations(updatedConfigurations);
        }
    }

    public MerchantModel getMerchantModelByMerchantId(String merchantId) {
        return merchantRepository
                .findById(merchantId)
                .orElseThrow(() -> new CommonApiException(
                        HttpStatus.NOT_FOUND,
                        new Error("merchant-not-found", "merchant with value: [" + merchantId + "] not found")
                ));
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

    public String getConfigurationValueByMerchantIdAndKey(String merchantId, String key) {
        MerchantModel model = getMerchantModelByMerchantId(merchantId);
        if (model == null) {
            throw new CommonApiException(
                    HttpStatus.NOT_FOUND,
                    new Error("merchant-not-found", "merchant with value: [" + merchantId + "] not found")
            );
        }
        Optional<ConfigurationModel> configuration = getConfigurationWithKeyFromModel(key, model);
        return configuration.map(ConfigurationModel::getValue).orElse(null);
    }

    /**
     * > Find the merchant with the given merchantId and namespace, then find the configuration with the given key, and
     * return the configuration
     *
     * @param merchantId The merchantId of the merchant you want to get the configuration value for.
     * @param namespace  The namespace of the configuration.
     * @param key        The key of the configuration value you want to retrieve.
     * @return ConfigurationDto of configuration value or null
     */
    public ConfigurationDto getConfigurationByMerchantIdAndNamespaceAndKey(String merchantId, String namespace, String key) {
        MerchantModel model = merchantRepository.findByMerchantIdAndNamespace(merchantId, namespace);

        Optional<ConfigurationModel> configuration = getConfigurationWithKeyFromModel(key, model);
        if (configuration.isPresent()) {
            return configurationMapper.toDto(configuration.get());
        }
        return null;
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
                .map(merchantMapper::toDto)
                .collect(Collectors.toList());
    }

    public MerchantDto findByMerchantIdAndNamespace(String merchantId, String namespace) {
        MerchantModel model = merchantRepository.findByMerchantIdAndNamespace(merchantId, namespace);
        if (model == null) {
            throw new CommonApiException(
                    HttpStatus.NOT_FOUND,
                    new Error("merchant-not-found", "merchant with value: [" + merchantId + "] not found")
            );
        }
        return merchantMapper.toDto(model);
    }

    public List<MerchantDto> initializeTestData() {
        String mockbackendurl = env.getProperty("mockbackend.url");

        MerchantModel asukaspysakointiMerchant = new MerchantModel();
        asukaspysakointiMerchant.setNamespace("asukaspysakointi");
        asukaspysakointiMerchant.setCreatedAt(DateTimeUtil.getFormattedDateTime());
        asukaspysakointiMerchant.setUpdatedAt(DateTimeUtil.getFormattedDateTime());

        MerchantModel venepaikatMerchant = new MerchantModel();
        venepaikatMerchant.setNamespace("venepaikat");
        venepaikatMerchant.setCreatedAt(DateTimeUtil.getFormattedDateTime());
        venepaikatMerchant.setUpdatedAt(DateTimeUtil.getFormattedDateTime());

        // Add configurations
        List<ConfigurationModel> asukaspysakointiConfig = Arrays.asList(
                constructConfigByParams(asukaspysakointiMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_NAME, "asukaspysäköinti", false),
                constructConfigByParams(asukaspysakointiMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_STREET, "Katu 1", false),
                constructConfigByParams(asukaspysakointiMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_ZIP, "000000", false),
                constructConfigByParams(asukaspysakointiMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_CITY, "Helsinki", false),
                constructConfigByParams(asukaspysakointiMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_EMAIL, "asukas@pysäköinti.fi", false),
                constructConfigByParams(asukaspysakointiMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_PHONE, "123-456789", false),
                constructConfigByParams(asukaspysakointiMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_URL, mockbackendurl+"/mockserviceconfiguration/asukaspysakointi/url", false),
                constructConfigByParams(asukaspysakointiMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_SHOP_ID, "695874", false), // Value is from paytrail documentation test Shop-in-Shop merchant ID
                constructConfigByParams(asukaspysakointiMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_PAYTRAIL_MERCHANT_ID, "375917", false)
                );

        List<ConfigurationModel> venepaikatConfig = Arrays.asList(
                constructConfigByParams(venepaikatMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_NAME, "venepaikat", false),
                constructConfigByParams(venepaikatMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_STREET, "Ranta 1", false),
                constructConfigByParams(venepaikatMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_ZIP, "000000", false),
                constructConfigByParams(venepaikatMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_CITY, "Helsinki", false),
                constructConfigByParams(venepaikatMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_EMAIL, "vene@paikat.fi", false),
                constructConfigByParams(venepaikatMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_PHONE, "123-456789", false),
                constructConfigByParams(venepaikatMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_URL, mockbackendurl+"/mockserviceconfiguration/venepaikat/url", false),
                constructConfigByParams(venepaikatMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_SHOP_ID, "695874", false), // Value is from paytrail documentation test Shop-in-Shop merchant ID
                constructConfigByParams(asukaspysakointiMerchant.getNamespace(), ServiceConfigurationKeys.MERCHANT_PAYTRAIL_MERCHANT_ID, "375917", false)
        );

        asukaspysakointiMerchant.setConfigurations(new ArrayList(asukaspysakointiConfig));
        venepaikatMerchant.setConfigurations(new ArrayList(venepaikatConfig));

        List<MerchantModel> MerchantEntities = Arrays.asList(asukaspysakointiMerchant, venepaikatMerchant);

        Iterable<MerchantModel> savedMerchantsIter = merchantRepository.saveAll(MerchantEntities);
        List<MerchantDto> savedMerchantDtos = StreamSupport.stream(savedMerchantsIter.spliterator(), false)
                .map(merchantMapper::toDto)
                .collect(Collectors.toList());

        log.debug("initialized namespace configurations mock data");
        return savedMerchantDtos;
    }

    public ConfigurationModel constructConfigByParams(String namespace, String configurationKey, String configurationValue, boolean isRestricted) {
        LocaleModel locale = new LocaleModel();
        locale.setFi("locale_fi");
        locale.setEn("locale_en");
        locale.setSv("locale_sv");
        ConfigurationModel config = new ConfigurationModel(configurationKey, configurationValue, isRestricted, locale);
        log.debug("created configuration for merchant namespace: " + namespace + " with configuration: " + config.toString());

        return config;
    }

    public MerchantDto addMerchantConfiguration(String merchantId, String configurationKey, String configurationValue) {
        MerchantModel foundMerchant = getMerchantModelByMerchantId(merchantId);
        ArrayList<ConfigurationModel> updateConfig = new ArrayList<>();
        updateConfig.add(
                constructConfigByParams(
                        foundMerchant.getNamespace(),
                        configurationKey,
                        configurationValue,
                        false
                )
        );
        MerchantDto updateDto = new MerchantDto();
        updateDto.setMerchantId(merchantId);
        updateDto.setConfigurations(updateConfig);
        return update(updateDto);
    }
}
