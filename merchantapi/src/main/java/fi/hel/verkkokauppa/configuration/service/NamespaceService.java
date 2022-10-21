package fi.hel.verkkokauppa.configuration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.configuration.api.namespace.dto.NamespaceDto;
import fi.hel.verkkokauppa.configuration.mapper.NamespaceMapper;
import fi.hel.verkkokauppa.configuration.model.ConfigurationModel;
import fi.hel.verkkokauppa.configuration.model.LocaleModel;
import fi.hel.verkkokauppa.configuration.model.namespace.NamespaceModel;
import fi.hel.verkkokauppa.configuration.repository.NamespaceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class NamespaceService {

    @Autowired
    private NamespaceMapper namespaceMapper;

    @Autowired
    private NamespaceRepository namespaceRepository;

    @Autowired
    private Environment env;

    // https://webhook.site/ To create custom unique webhook test url, makes webhook testing easier
    @Value("${webhook.url:#{null}}")
    private String webHookUrl;

    /**
     * Save the namespace and return the saved namespace as a DTO.
     *
     * @param dto The DTO object that will be saved to the database.
     * @return A NamespaceDto object
     */
    public NamespaceDto save(NamespaceDto dto) {
        NamespaceModel entity = namespaceMapper.fromDto(dto);
        isDuplicateNamespace(dto);
        entity.setCreatedAt(DateTimeUtil.getFormattedDateTime());
        NamespaceModel saved = namespaceRepository.save(entity);
        return namespaceMapper.toDto(saved);
    }

    private void isDuplicateNamespace(NamespaceDto dto) {
        if (namespaceRepository.findByNamespace(dto.getNamespace()).isPresent()) {
            throw new CommonApiException(
                    HttpStatus.BAD_REQUEST,
                    new Error("duplicate-namespace", "namespace with value: [" + dto.getNamespace() + "] is a duplicate one")
            );
        }
    }


    /**
     * > Update a namespace entity from a DTO, and return the updated DTO
     *
     * @param dto The DTO object that contains the data to be updated.
     * @return NamespaceDto
     */
    public NamespaceDto update(NamespaceDto dto) throws CommonApiException {
        NamespaceModel entity = getNamespaceModelByNamespace(dto.getNamespace());

        NamespaceModel updatedEntity;
        try {
            // Get original configurations before appending/updating
            ArrayList<ConfigurationModel> originalDtoConfigurations = dto.getConfigurations();
            // List of configurations that are added to model
            ArrayList<ConfigurationModel> updatedConfigurations = new ArrayList<>();
            // Model existing configurations
            ArrayList<ConfigurationModel> existingModelConfigurations = entity.getConfigurations();

            updateConfigurations(dto, entity, originalDtoConfigurations, updatedConfigurations, existingModelConfigurations);

            updatedEntity = namespaceMapper.updateFromDtoToModel(entity, dto);

            updatedEntity.setUpdatedAt(DateTimeUtil.getFormattedDateTime());
        } catch (JsonProcessingException e) {
            throw new CommonApiException(
                    HttpStatus.NOT_FOUND,
                    new Error("namespace-dto-json-error", "namespace with value: [" + dto.getNamespace() + "] json processing error")
            );
        }

        return namespaceMapper.toDto(namespaceRepository.save(updatedEntity));
    }

    /**
     * Checking if the existing model configurations are empty. If not, it is iterating through the existing model
     * configurations and checking if the key is present in the dto configurations. If it is, it is adding the dto
     * configuration to the updated configurations. If not, it is adding the existing model configuration to the
     * updated configurations.
     */
    private void updateConfigurations(NamespaceDto dto, NamespaceModel entity, ArrayList<ConfigurationModel> originalDtoConfigurations, ArrayList<ConfigurationModel> updatedConfigurations, ArrayList<ConfigurationModel> existingModelConfigurations) {
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

    private NamespaceModel getNamespaceModelByNamespace(String namespace) {
        return namespaceRepository
                .findByNamespace(namespace)
                .orElseThrow(() -> new CommonApiException(
                        HttpStatus.NOT_FOUND,
                        new Error("namespace-not-found", "namespace with value: [" + namespace + "] not found")
                ));
    }

    /**
     * > Find the namespace with the given namespace, then find the configuration with the given key, and
     * return the value of that configuration
     *
     * @param namespace The namespace of the configuration.
     * @param key       The key of the configuration value you want to retrieve.
     * @return A String of configuration value or null
     */
    public String getConfigurationValueByNamespaceAndKey(String namespace, String key) {
        NamespaceModel model = getNamespaceModelByNamespace(namespace);

        Optional<ConfigurationModel> configuration = getConfigurationWithKeyFromModel(key, model);

        return configuration.map(ConfigurationModel::getValue).orElse(null);
    }

    public Optional<ConfigurationModel> getConfigurationWithKeyFromModel(String key, NamespaceModel model) {
        return model.getConfigurations()
                .stream()
                .filter(configurationModel -> Objects.equals(configurationModel.getKey(), key))
                .findFirst();
    }

    public NamespaceDto findByNamespace(String namespace) {
        return namespaceMapper.toDto(getNamespaceModelByNamespace(namespace));
    }

    public List<NamespaceDto> initializeTestData() {
        String mockbackendurl = env.getProperty("mockbackend.url");

        NamespaceModel asukaspysakointiNamespace = new NamespaceModel();
        asukaspysakointiNamespace.setNamespace("asukaspysakointi");
        asukaspysakointiNamespace.setCreatedAt(DateTimeUtil.getFormattedDateTime());
        asukaspysakointiNamespace.setUpdatedAt(DateTimeUtil.getFormattedDateTime());

        NamespaceModel venepaikatNamespace = new NamespaceModel();
        venepaikatNamespace.setNamespace("venepaikat");
        venepaikatNamespace.setCreatedAt(DateTimeUtil.getFormattedDateTime());
        venepaikatNamespace.setUpdatedAt(DateTimeUtil.getFormattedDateTime());

        // Add configurations
        List<ConfigurationModel> asukaspysakointiConfig = Arrays.asList(
            constructConfigByParams(asukaspysakointiNamespace.getNamespace(), ServiceConfigurationKeys.PAYMENT_API_KEY, "asukaspysakointi_mock_api_key", true),
            constructConfigByParams(asukaspysakointiNamespace.getNamespace(), ServiceConfigurationKeys.PAYMENT_ENCRYPTION_KEY, "asukaspysakointi_mock_encryption_key", true),
            constructConfigByParams(asukaspysakointiNamespace.getNamespace(), ServiceConfigurationKeys.PAYMENT_RETURN_URL, mockbackendurl + "/mockserviceconfiguration/asukaspysakointi/return_url", true),
            constructConfigByParams(asukaspysakointiNamespace.getNamespace(), ServiceConfigurationKeys.PAYMENT_NOTIFICATION_URL, mockbackendurl + "/mockserviceconfiguration/asukaspysakointi/notification_url", false),
            constructConfigByParams(asukaspysakointiNamespace.getNamespace(), ServiceConfigurationKeys.PAYMENT_SUBMERCHANT_ID, "36240", true),
            constructConfigByParams(asukaspysakointiNamespace.getNamespace(), ServiceConfigurationKeys.PAYMENT_CP, "PRO-31312-1", true),
            constructConfigByParams(asukaspysakointiNamespace.getNamespace(), ServiceConfigurationKeys.MERCHANT_TERMS_OF_SERVICE_URL, mockbackendurl+"/mockserviceconfiguration/asukaspysakointi/terms_of_use", false),
            constructConfigByParams(asukaspysakointiNamespace.getNamespace(), ServiceConfigurationKeys.ORDER_RIGHT_OF_PURCHASE_IS_ACTIVE, "true", false),
            constructConfigByParams(asukaspysakointiNamespace.getNamespace(), ServiceConfigurationKeys.ORDER_RIGHT_OF_PURCHASE_URL, mockbackendurl+"/mock/asukaspysakointi/order/right-of-purchase", false),
            constructConfigByParams(asukaspysakointiNamespace.getNamespace(), ServiceConfigurationKeys.SUBSCRIPTION_PRICE_URL, mockbackendurl+"/mock/asukaspysakointi/subscription/price", false),
            // Webhooks
            constructConfigByParams(asukaspysakointiNamespace.getNamespace(), ServiceConfigurationKeys.MERCHANT_PAYMENT_WEBHOOK_URL, webHookUrl != null ? webHookUrl : mockbackendurl + "/mockserviceconfiguration/asukaspysakointi/merchant_payment_webhook", false),
            constructConfigByParams(asukaspysakointiNamespace.getNamespace(), ServiceConfigurationKeys.MERCHANT_ORDER_WEBHOOK_URL, webHookUrl != null ? webHookUrl : mockbackendurl + "/mockserviceconfiguration/asukaspysakointi/merchant_order_webhook", false),
            constructConfigByParams(asukaspysakointiNamespace.getNamespace(), ServiceConfigurationKeys.MERCHANT_SUBSCRIPTION_WEBHOOK_URL, webHookUrl != null ? webHookUrl : mockbackendurl + "/mockserviceconfiguration/asukaspysakointi/merchant_subscription_webhook", false)
        );

        List<ConfigurationModel> venepaikatConfig = Arrays.asList(
            constructConfigByParams(venepaikatNamespace.getNamespace(), ServiceConfigurationKeys.PAYMENT_API_KEY, "venepaikat_mock_api_key", true),
            constructConfigByParams(venepaikatNamespace.getNamespace(), ServiceConfigurationKeys.PAYMENT_ENCRYPTION_KEY, "venepaikat_mock_encryption_key", true),
            constructConfigByParams(venepaikatNamespace.getNamespace(), ServiceConfigurationKeys.PAYMENT_RETURN_URL, mockbackendurl + "/mockserviceconfiguration/venepaikat/return_url", true),
            constructConfigByParams(venepaikatNamespace.getNamespace(), ServiceConfigurationKeys.PAYMENT_NOTIFICATION_URL, mockbackendurl + "/mockserviceconfiguration/venepaikat/notification_url", true),
            constructConfigByParams(venepaikatNamespace.getNamespace(), ServiceConfigurationKeys.PAYMENT_SUBMERCHANT_ID, "36240", true),
            constructConfigByParams(venepaikatNamespace.getNamespace(), ServiceConfigurationKeys.PAYMENT_CP, "PRO-31312-1", true),
            constructConfigByParams(venepaikatNamespace.getNamespace(), ServiceConfigurationKeys.MERCHANT_TERMS_OF_SERVICE_URL, mockbackendurl+"/mockserviceconfiguration/venepaikat/terms_of_use", false),
            constructConfigByParams(venepaikatNamespace.getNamespace(), ServiceConfigurationKeys.ORDER_RIGHT_OF_PURCHASE_IS_ACTIVE, "true", false),
            constructConfigByParams(venepaikatNamespace.getNamespace(), ServiceConfigurationKeys.ORDER_RIGHT_OF_PURCHASE_URL, mockbackendurl+"/mock/venepaikat/order/right-of-purchase", false),
            // Webhooks
            constructConfigByParams(venepaikatNamespace.getNamespace(), ServiceConfigurationKeys.MERCHANT_PAYMENT_WEBHOOK_URL, webHookUrl != null ? webHookUrl : mockbackendurl + "/mockserviceconfiguration/venepaikat/merchant_payment_webhook", false),
            constructConfigByParams(venepaikatNamespace.getNamespace(), ServiceConfigurationKeys.MERCHANT_ORDER_WEBHOOK_URL, webHookUrl != null ? webHookUrl : mockbackendurl + "/mockserviceconfiguration/venepaikat/merchant_order_webhook", false),
            constructConfigByParams(venepaikatNamespace.getNamespace(), ServiceConfigurationKeys.MERCHANT_SUBSCRIPTION_WEBHOOK_URL, webHookUrl != null ? webHookUrl : mockbackendurl + "/mockserviceconfiguration/venepaikat/merchant_subscription_webhook", false)
        );

        asukaspysakointiNamespace.setConfigurations(new ArrayList(asukaspysakointiConfig));
        venepaikatNamespace.setConfigurations(new ArrayList(venepaikatConfig));

        List<NamespaceModel> namespaceEntities = Arrays.asList(asukaspysakointiNamespace, venepaikatNamespace);

        Iterable<NamespaceModel> savedNamespacesIter = namespaceRepository.saveAll(namespaceEntities);
        List<NamespaceDto> savedNamespaceDtos = StreamSupport.stream(savedNamespacesIter.spliterator(), false)
                        .map(namespaceMapper::toDto)
                        .collect(Collectors.toList());

        log.debug("initialized namespace configurations mock data");
        return savedNamespaceDtos;
    }

    public ConfigurationModel constructConfigByParams(String namespace, String configurationKey, String configurationValue, boolean isRestricted) {
        LocaleModel locale = new LocaleModel();
        locale.setFi("locale_fi");
        locale.setEn("locale_en");
        locale.setSv("locale_sv");
        ConfigurationModel config = new ConfigurationModel(configurationKey, configurationValue, isRestricted, locale);
        log.debug("created configuration for namespace: " + namespace + " with configuration: " + config.toString());

        return config;
    }
}
