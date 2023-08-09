package fi.hel.verkkokauppa.configuration.api.merchant;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.configuration.api.merchant.dto.MerchantDto;
import fi.hel.verkkokauppa.configuration.api.merchant.dto.PaytrailMerchantMappingDto;
import fi.hel.verkkokauppa.configuration.model.ConfigurationModel;
import fi.hel.verkkokauppa.configuration.model.LocaleModel;
import fi.hel.verkkokauppa.configuration.repository.MerchantRepository;
import fi.hel.verkkokauppa.configuration.repository.PaytrailMerchantMappingRepository;
import fi.hel.verkkokauppa.configuration.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Slf4j
@TestPropertySource(properties = {
        "merchant.secret.encryption.salt=test-salt",
})
public class MerchantControllerTest {
    private ArrayList<String> toBeDeleted = new ArrayList<>();
    private ArrayList<String> toBeDeletedPaytrailMerchantMapping = new ArrayList<>();
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MerchantController merchantController;
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private PaytrailMerchantMappingRepository paytrailMerchantMappingRepository;

    @AfterEach
    void tearDown() {
        try {
            toBeDeleted.forEach(s -> merchantRepository.deleteById(s));
            // Clear list because all merchants deleted
            toBeDeleted = new ArrayList<>();
            toBeDeletedPaytrailMerchantMapping.forEach(s -> paytrailMerchantMappingRepository.deleteById(s));
            toBeDeletedPaytrailMerchantMapping = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
        }

    }

    /**
     * It tests the upsert merchant endpoint.
     */
    @Test
    @RunIfProfile(profile = "local")
    public void returnsError404IfMerchantIsNotFoundTest() throws Exception {
        MerchantDto merchantDto = new MerchantDto();
        merchantDto.setMerchantId("merchantId");
        merchantDto.setNamespace("namespace");

        ConfigurationModel configurationModel = new ConfigurationModel();

        ArrayList<ConfigurationModel> configurationModels = new ArrayList<>();
        configurationModels.add(configurationModel);

        merchantDto.setConfigurations(configurationModels);

        CommonApiException exception = assertThrows(CommonApiException.class, () -> {
            merchantController.upsertMerchant(merchantDto);
        });

        assertEquals(CommonApiException.class, exception.getClass());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("merchant-not-found", exception.getErrors().getErrors().get(0).getCode());
        assertEquals("merchant with value: [merchantId] not found", exception.getErrors().getErrors().get(0).getMessage());
    }


    @Test
    @RunIfProfile(profile = "local")
    public void throwsError400IfEmptyNamespace() throws Exception {
        MerchantDto merchantDto = new MerchantDto();

        ConfigurationModel configurationModel = new ConfigurationModel();

        ArrayList<ConfigurationModel> configurationModels = new ArrayList<>();
        configurationModels.add(configurationModel);

        merchantDto.setConfigurations(configurationModels);
        //javax.validation.ConstraintViolationException: upsertMerchant.merchantDto.namespace: namespace cant be blank

        Exception exception = assertThrows(ConstraintViolationException.class, () -> {
            AtomicReference<ResponseEntity<MerchantDto>> response = new AtomicReference<>(merchantController.upsertMerchant(merchantDto));
            Assertions.assertEquals(response.get().getStatusCode(), HttpStatus.BAD_REQUEST);
            Assertions.assertNull(response.get().getBody());
        });
        Assertions.assertEquals(exception.getMessage(), "upsertMerchant.merchantDto.namespace: namespace cant be blank");

    }

    @Test
    @RunIfProfile(profile = "local")
    public void upsertWithValidDataReturnsStatus200() throws Exception {
        MerchantDto merchantDto = new MerchantDto();
        merchantDto.setNamespace("test-namespace");

        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setKey("test-key");
        configurationModel.setValue("test-value");

        LocaleModel locale = new LocaleModel();
        locale.setFi("test-fi");
        locale.setSv("test-sv");
        locale.setEn("test-en");
        configurationModel.setLocale(locale);

        ArrayList<ConfigurationModel> configurationModels = new ArrayList<>();
        configurationModels.add(configurationModel);

        merchantDto.setConfigurations(configurationModels);

        ResponseEntity<MerchantDto> response = merchantController.upsertMerchant(merchantDto);

        MerchantDto responseMerchantDto = response.getBody();
        Assertions.assertNotNull(responseMerchantDto);
        String merchantId = responseMerchantDto.getMerchantId();
        toBeDeleted.add(merchantId);
        Assertions.assertNotNull(merchantId);
        Assertions.assertNotNull(responseMerchantDto.getCreatedAt());
        Assertions.assertNotNull(responseMerchantDto.getConfigurations());
        Assertions.assertEquals(1, responseMerchantDto.getConfigurations().size());

        Assertions.assertEquals("test-namespace", responseMerchantDto.getNamespace());
        Assertions.assertEquals("test-key", responseMerchantDto.getConfigurations().get(0).getKey());
        Assertions.assertEquals("test-value", responseMerchantDto.getConfigurations().get(0).getValue());
        Assertions.assertEquals("test-fi", responseMerchantDto.getConfigurations().get(0).getLocale().getFi());
        Assertions.assertEquals("test-sv", responseMerchantDto.getConfigurations().get(0).getLocale().getSv());
        Assertions.assertEquals("test-en", responseMerchantDto.getConfigurations().get(0).getLocale().getEn());
    }


    @Test
    @RunIfProfile(profile = "local")
    public void getValueReturnsCorrectValueAndStatus200() throws Exception {
        MerchantDto merchantDto = new MerchantDto();
        String namespace = "test-namespace" + UUIDGenerator.generateType4UUID();
        merchantDto.setNamespace(namespace);

        ConfigurationModel configurationModel = new ConfigurationModel();

        String requestKey = "test-key";
        String requestValue = "test-value";

        configurationModel.setKey(requestKey);
        configurationModel.setValue(requestValue);

        ArrayList<ConfigurationModel> configurationModels = new ArrayList<>() {{
            add(configurationModel);
        }};


        merchantDto.setConfigurations(configurationModels);

        ResponseEntity<MerchantDto> response = merchantController.upsertMerchant(merchantDto);

        MerchantDto responseMerchantDto = response.getBody();
        Assertions.assertNotNull(responseMerchantDto);
        String merchantId = responseMerchantDto.getMerchantId();
        toBeDeleted.add(merchantId);
        Assertions.assertNotNull(merchantId);
        Assertions.assertEquals(1, responseMerchantDto.getConfigurations().size());
        Assertions.assertEquals(namespace, responseMerchantDto.getNamespace());

        ConfigurationModel responseConfiguration = responseMerchantDto.getConfigurations().get(0);

        String responseKey = responseConfiguration.getKey();
        String responseValue = responseConfiguration.getValue();

        Assertions.assertEquals(requestKey, responseKey);
        Assertions.assertEquals(requestValue, responseValue);

        ResponseEntity<String> getValueResponse = merchantController.getValue(
                responseMerchantDto.getMerchantId(),
                namespace,
                requestKey
        );
        log.info("getValueResponse : {}", getValueResponse);
        Assertions.assertEquals(requestValue, getValueResponse.getBody());

        ResponseEntity<String> getValueResponseShouldBeNull = merchantController.getValue(
                responseMerchantDto.getMerchantId(),
                namespace,
                "KEY_SHOULD_NOT_BE_FOUND"
        );

        log.info("getValueResponseShouldBeNull : {}", getValueResponseShouldBeNull);
        Assertions.assertNull(getValueResponseShouldBeNull.getBody());

    }

    @Test
    @RunIfProfile(profile = "local")
    public void getMerchantsByNamespace() throws Exception {
        MerchantDto merchantDto = new MerchantDto();
        String namespace = "test-namespace" + UUIDGenerator.generateType4UUID();
        merchantDto.setNamespace(namespace);
        ConfigurationModel configurationModel = new ConfigurationModel();

        String requestKey = "test-key";
        String requestValue = "test-value";

        configurationModel.setKey(requestKey);
        configurationModel.setValue(requestValue);
        ArrayList<ConfigurationModel> configurationModels = new ArrayList<>() {{
            add(configurationModel);
        }};

        merchantDto.setConfigurations(configurationModels);
        // Create 2 merchants
        ResponseEntity<MerchantDto> response = merchantController.upsertMerchant(merchantDto);
        ResponseEntity<MerchantDto> response2 = merchantController.upsertMerchant(merchantDto);

        MerchantDto responseMerchantDto = response.getBody();
        MerchantDto responseMerchantDto2 = response2.getBody();
        Assertions.assertNotNull(responseMerchantDto);
        String merchantId = responseMerchantDto.getMerchantId();
        String merchantId2 = Objects.requireNonNull(responseMerchantDto2).getMerchantId();
        toBeDeleted.add(merchantId);
        toBeDeleted.add(merchantId2);
        Assertions.assertNotNull(merchantId);
        Assertions.assertNotNull(merchantId);
        Assertions.assertEquals(1, responseMerchantDto.getConfigurations().size());
        Assertions.assertEquals(namespace, responseMerchantDto.getNamespace());

        ConfigurationModel responseConfiguration = responseMerchantDto.getConfigurations().get(0);

        String responseKey = responseConfiguration.getKey();
        String responseValue = responseConfiguration.getValue();

        Assertions.assertEquals(requestKey, responseKey);
        Assertions.assertEquals(requestValue, responseValue);

        ResponseEntity<List<MerchantDto>> merchantsByNamespace = merchantController.getMerchantsByNamespace(namespace);

        Assertions.assertEquals(merchantsByNamespace.getStatusCode(), HttpStatus.OK);
        Assertions.assertEquals(2, Objects.requireNonNull(merchantsByNamespace.getBody()).size());

        ResponseEntity<List<MerchantDto>> merchantsByNamespaceShouldBeEmptyList = merchantController.getMerchantsByNamespace(UUIDGenerator.generateType4UUID().toString());
        Assertions.assertEquals(0, Objects.requireNonNull(merchantsByNamespaceShouldBeEmptyList.getBody()).size());

    }

    @Test
    @RunIfProfile(profile = "local")
    public void merchantGetKeys() {
        ResponseEntity<List<String>> responseMerchantKeys = merchantController.getKeys();
        List<String> allKeys = new ArrayList<>(new ArrayList<>() {{
            add("merchantCity");
            add("merchantEmail");
            add("merchantName");
            add("merchantPaytrailMerchantId");
            add("merchantPhone");
            add("merchantShopId");
            add("merchantStreet");
            add("merchantTermsOfServiceUrl");
            add("merchantUrl");
            add("merchantZip");
            add("orderRightOfPurchaseIsActive");
            add("orderRightOfPurchaseUrl");
            add("subscriptionPriceUrl");
        }});
        Assertions.assertEquals(allKeys, responseMerchantKeys.getBody());
    }

    @Test
    @RunIfProfile(profile = "local")
    public void upsertWithValidDataAndCreatePaytrailSecret() throws Exception {
        MerchantDto merchantDto = new MerchantDto();
        merchantDto.setNamespace("test-namespace");

        ConfigurationModel configurationModel = new ConfigurationModel();

        createLocalModel(configurationModel);

        createEmptyConfigurationsModel(merchantDto, configurationModel);

        ResponseEntity<MerchantDto> response = merchantController.upsertMerchant(merchantDto);

        MerchantDto responseMerchantDto = response.getBody();
        Assertions.assertNotNull(responseMerchantDto);
        String merchantId = responseMerchantDto.getMerchantId();
        toBeDeleted.add(merchantId);
        Assertions.assertNotNull(merchantId);
        // Just an empty configuration
        Assertions.assertEquals(1, responseMerchantDto.getConfigurations().size());

        String testSecret = "test-secret-value";
        ResponseEntity<MerchantDto> merchantDtoResponseEntity = merchantController.addPaytrailSecret(merchantId, testSecret);
        MerchantDto addedSecretDto = merchantDtoResponseEntity.getBody();
        assert addedSecretDto != null;
        Assertions.assertEquals(2, addedSecretDto.getConfigurations().size());
        ConfigurationModel secretConfiguration = addedSecretDto.getConfigurations().get(1);
        Assertions.assertEquals(ServiceConfigurationKeys.MERCHANT_PAYTRAIL_SECRET, secretConfiguration.getKey());
        Assertions.assertNotEquals(testSecret, secretConfiguration.getValue());
        ResponseEntity<String> paytrailSecretResponse = merchantController.getPaytrailSecret(merchantId);
        Assertions.assertEquals(testSecret, paytrailSecretResponse.getBody());

    }

    @Test
    @RunIfProfile(profile = "local")
    public void addPaytrailSecretWhenPaytrailMerchantMappingExists() {
        // Add paytrail merchant mapping
        PaytrailMerchantMappingDto paytrailMerchantMappingDto = new PaytrailMerchantMappingDto();
        paytrailMerchantMappingDto.setMerchantPaytrailMerchantId("pmid1");
        paytrailMerchantMappingDto.setMerchantPaytrailSecret("secret123");
        paytrailMerchantMappingDto.setNamespace("ns1");
        ResponseEntity<PaytrailMerchantMappingDto> addPaytrailMerchantMappingResponse = merchantController.addPaytrailMerchantMapping(paytrailMerchantMappingDto);
        paytrailMerchantMappingDto = addPaytrailMerchantMappingResponse.getBody();
        Assertions.assertNotNull(paytrailMerchantMappingDto);
        toBeDeletedPaytrailMerchantMapping.add(paytrailMerchantMappingDto.getId());
        // Upsert merchant
        MerchantDto merchantDto = new MerchantDto();
        merchantDto.setNamespace("ns1");
        merchantDto.setMerchantPaytrailMerchantId("pmid1");
        ConfigurationModel configurationModel = new ConfigurationModel();
        createLocalModel(configurationModel);
        createEmptyConfigurationsModel(merchantDto, configurationModel);
        ResponseEntity<MerchantDto> response = merchantController.upsertMerchant(merchantDto);
        MerchantDto responseMerchantDto = response.getBody();
        Assertions.assertNotNull(responseMerchantDto);
        String merchantId = responseMerchantDto.getMerchantId();
        toBeDeleted.add(merchantId);
        Assertions.assertNotNull(merchantId);
        Assertions.assertEquals(1, responseMerchantDto.getConfigurations().size());
        // Get paytrail secret and assert it matches the paytrail merchant mapping
        ResponseEntity<String> getPaytrailSecretResponse = merchantController.getPaytrailSecret(merchantId);
        String paytrailSecret = getPaytrailSecretResponse.getBody();
        Assertions.assertEquals(paytrailSecret, "secret123");
    }

    private void createEmptyConfigurationsModel(MerchantDto merchantDto, ConfigurationModel configurationModel) {
        ArrayList<ConfigurationModel> configurationModels = new ArrayList<>();
        configurationModels.add(configurationModel);

        merchantDto.setConfigurations(configurationModels);
    }

    private void createLocalModel(ConfigurationModel configurationModel) {
        LocaleModel locale = new LocaleModel();
        locale.setFi("test-fi");
        locale.setSv("test-sv");
        locale.setEn("test-en");
        configurationModel.setLocale(locale);
    }

}
