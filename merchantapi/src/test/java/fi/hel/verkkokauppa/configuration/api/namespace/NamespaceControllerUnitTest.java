package fi.hel.verkkokauppa.configuration.api.namespace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.configuration.api.merchant.MerchantController;
import fi.hel.verkkokauppa.configuration.api.merchant.dto.MerchantDto;
import fi.hel.verkkokauppa.configuration.mapper.MerchantMapper;
import fi.hel.verkkokauppa.configuration.model.ConfigurationModel;
import fi.hel.verkkokauppa.configuration.model.LocaleModel;
import fi.hel.verkkokauppa.configuration.model.merchant.MerchantModel;
import fi.hel.verkkokauppa.configuration.repository.MerchantRepository;
import fi.hel.verkkokauppa.configuration.service.MerchantService;
import fi.hel.verkkokauppa.configuration.testing.utils.AutoMockBeanFactory;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * This class is used to test the controller layer of the application
 * <p>
 * Change MerchantController.class to controller which you want to test.
 */
@WebMvcTest(MerchantController.class) // Change and uncomment
@Import(MerchantController.class) // Change and uncomment
@ContextConfiguration(classes = AutoMockBeanFactory.class) // This automatically mocks missing beans
@AutoConfigureMockMvc // This activates auto configuration to call mocked api endpoints.
@Slf4j
@EnableAutoConfiguration(exclude = {
        ActiveMQAutoConfiguration.class,
        KafkaAutoConfiguration.class
})
public class NamespaceControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // You need to add all dependencies in controller with @Autowired annotation
    // as new field with @MockBean to controller test.
    @MockBean
    private MerchantService merchantService;

    @MockBean
    private MerchantRepository merchantRepository;

    @MockBean
    private MerchantMapper merchantMapper;

    /**
     * It tests the upsert merchant endpoint.
     */
    @Test
    public void throwsError500IfMerchantIsNotFoundTest() throws Exception {
        MerchantDto merchantDto = new MerchantDto();
        merchantDto.setMerchantId("merchantId");
        merchantDto.setNamespace("namespace");

        Mockito.when(merchantService.update(merchantDto)).thenCallRealMethod();

        ConfigurationModel configurationModel = new ConfigurationModel();

        ArrayList<ConfigurationModel> configurationModels = new ArrayList<>();
        configurationModels.add(configurationModel);

        merchantDto.setConfigurations(configurationModels);

        Exception exception = assertThrows(Exception.class, () -> {
            this.mockMvc.perform(
                            post("/merchant/upsert")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(merchantDto))
                    )
                    .andDo(print())
                    .andExpect(status().is5xxServerError());
        });

        CommonApiException cause = (CommonApiException) exception.getCause();
        assertEquals(CommonApiException.class, cause.getClass());

        assertEquals("merchant-not-found", cause.getErrors().getErrors().get(0).getCode());
        assertEquals("merchant with id [merchantId] not found", cause.getErrors().getErrors().get(0).getMessage());
    }


    @Test
    public void throwsError400IfEmptyNamespace() throws Exception {
        MerchantDto merchantDto = new MerchantDto();

        Mockito.when(merchantService.update(merchantDto)).thenCallRealMethod();

        ConfigurationModel configurationModel = new ConfigurationModel();

        ArrayList<ConfigurationModel> configurationModels = new ArrayList<>();
        configurationModels.add(configurationModel);

        merchantDto.setConfigurations(configurationModels);


        MvcResult response = this.mockMvc.perform(
                        post("/merchant/upsert")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(merchantDto))
                )
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andReturn();

        MatcherAssert.assertThat(Objects.requireNonNull(response.getResolvedException()).getMessage(), Matchers.containsString("[Field error in object 'merchantDto' on field 'namespace': rejected value [null]"));
    }

    @Test
    public void upsertWithValidDataReturnsStatus200() throws Exception {
        MerchantDto merchantDto = new MerchantDto();
        merchantDto.setNamespace("test-namespace");

        ReflectionTestUtils.setField(merchantMapper, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(merchantService, "mapper", merchantMapper);
        ReflectionTestUtils.setField(merchantService, "merchantRepository", merchantRepository);
        Mockito.when(merchantMapper.fromDto(merchantDto)).thenCallRealMethod();
        Mockito.when(merchantService.save(merchantDto)).thenCallRealMethod();

        Mockito.when(merchantRepository.save(any(MerchantModel.class))).thenAnswer(i -> i.getArguments()[0]);
        Mockito.when(merchantMapper.toDto(any(MerchantModel.class))).thenAnswer(i -> objectMapper.convertValue(i.getArguments()[0], MerchantDto.class));

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

        MvcResult response = this.mockMvc.perform(
                        post("/merchant/upsert")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(merchantDto))
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        String contentAsString = response.getResponse().getContentAsString();
        MerchantDto responseMerchantDto = objectMapper.readValue(contentAsString, MerchantDto.class);
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

    // TODO NEW

    @Test
    public void getValueReturnsCorrectValueAndStatus200() throws Exception {
        MerchantDto merchantDto = new MerchantDto();
        String merchantId = "test-merchantId";
        merchantDto.setMerchantId(merchantId);
        String namespace = "test-namespace";
        merchantDto.setNamespace(namespace);

        ReflectionTestUtils.setField(merchantMapper, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(merchantService, "mapper", merchantMapper);
        ReflectionTestUtils.setField(merchantService, "merchantRepository", merchantRepository);

        String testValue = "test-value";
        String testKey = "test-key";
        Mockito.when(merchantService.getConfigurationValueByMerchantIdAndNamespaceAndKey(
                merchantId,
                namespace,
                testKey
        )).thenCallRealMethod();

        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setKey(testKey);
        configurationModel.setValue("test-value");

        Mockito.when(merchantService.getConfigurationWithKeyFromModel(any(), any())).thenReturn(Optional.of(configurationModel));

        Mockito.when(merchantRepository.findByMerchantIdAndNamespace(any(), any())).thenReturn(objectMapper.convertValue(merchantDto, MerchantModel.class));

        MvcResult response = this.mockMvc.perform(
                        get("/merchant/getValue")
                                .param("merchantId", merchantId)
                                .param("namespace", namespace)
                                .param("key", testKey)
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        String contentAsString = response.getResponse().getContentAsString();
        Assertions.assertEquals(testValue, contentAsString);
    }

    @Test
    public void getMerchantsByNamespace() throws Exception {
        MerchantDto merchantDto = new MerchantDto();
        String merchantId = "test-merchantId";
        merchantDto.setMerchantId(merchantId);
        String namespace = "test-namespace";
        merchantDto.setNamespace(namespace);

        String testValue = "test-value";
        String testKey = "test-key";

        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setKey(testKey);
        configurationModel.setValue(testValue);
        merchantDto.setConfigurations(new ArrayList<>() {{
            add(configurationModel);
        }});

        MerchantDto merchantDto2 = new MerchantDto();
        merchantDto2.setMerchantId(merchantId);
        merchantDto2.setNamespace(namespace);

        ConfigurationModel configurationModel2 = new ConfigurationModel();
        configurationModel2.setKey(testKey);
        configurationModel2.setValue(testValue);
        merchantDto2.setConfigurations(new ArrayList<>() {{
            add(configurationModel2);
        }});


        Mockito.when(merchantService.findAllByNamespace(eq(namespace))).thenReturn(
                new ArrayList<>() {{
                    add(merchantDto);
                    add(merchantDto2);
                }}
        );

        merchantService.findAllByNamespace(namespace);


        MvcResult response = this.mockMvc.perform(
                        get("/merchant/list-by-namespace")
                                .param("namespace", namespace)
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        ArrayList<MerchantDto> responseDtos = objectMapper.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
        Assertions.assertEquals(2, responseDtos.size());
    }

    @Test
    public void merchantGetKeys() throws Exception {

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

        MvcResult response = this.mockMvc.perform(
                        get("/merchant/keys")
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Assertions.assertEquals(objectMapper.writeValueAsString(allKeys), response.getResponse().getContentAsString());
    }


}
