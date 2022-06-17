package fi.hel.verkkokauppa.merchant.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.merchant.api.dto.MerchantDto;
import fi.hel.verkkokauppa.merchant.model.ConfigurationModel;
import fi.hel.verkkokauppa.merchant.model.LocaleModel;
import fi.hel.verkkokauppa.merchant.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


@SpringBootTest
@Slf4j
public class MerchantControllerTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MerchantController merchantController;

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
        Assertions.assertEquals(exception.getMessage(),"upsertMerchant.merchantDto.namespace: namespace cant be blank");


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
        Assertions.assertNotNull(responseMerchantDto.getMerchantId());
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

}
