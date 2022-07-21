package fi.hel.verkkokauppa.configuration.api.merchant;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.configuration.api.merchant.MerchantController;
import fi.hel.verkkokauppa.configuration.api.merchant.dto.MerchantDto;
import fi.hel.verkkokauppa.configuration.model.ConfigurationModel;
import fi.hel.verkkokauppa.configuration.model.LocaleModel;

import fi.hel.verkkokauppa.configuration.repository.MerchantRepository;
import fi.hel.verkkokauppa.configuration.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


@SpringBootTest
@Slf4j
public class MerchantControllerTest {
    private ArrayList<String> toBeDeleted = new ArrayList<>();
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MerchantController merchantController;
    @Autowired
    private MerchantRepository merchantRepository;

    @AfterEach
    void tearDown() {
        try {
            toBeDeleted.forEach(s -> merchantRepository.deleteById(s));
            // Clear list because all merchants deleted
            toBeDeleted = new ArrayList<>();
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
        assertEquals("merchant with id [merchantId] not found", exception.getErrors().getErrors().get(0).getMessage());
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
            add("merchantName");
            add("merchantStreet");
            add("merchantZip");
            add("merchantCity");
            add("merchantEmail");
            add("merchantPhone");
            add("merchantUrl");
            add("merchantTermsOfServiceUrl");
            add("merchantPaymentWebhookUrl");
            add("orderRightOfPurchaseIsActive");
            add("orderRightOfPurchaseUrl");
            add("merchantOrderWebhookUrl");
            add("merchantSubscriptionWebhookUrl");
            add("subscriptionPriceUrl");
            add("payment_api_version");
            add("payment_api_key");
            add("payment_currency");
            add("payment_type");
            add("payment_register_card_token");
            add("payment_return_url");
            add("payment_notification_url");
            add("payment_language");
            add("payment_submerchant_id");
            add("payment_cp");
        }});
        Assertions.assertEquals(allKeys, responseMerchantKeys.getBody());
    }

}
