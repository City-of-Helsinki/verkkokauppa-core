package fi.hel.verkkokauppa.merchant.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.merchant.api.dto.MerchantDto;
import fi.hel.verkkokauppa.merchant.mapper.MerchantMapper;
import fi.hel.verkkokauppa.merchant.model.ConfigurationModel;
import fi.hel.verkkokauppa.merchant.model.LocaleModel;
import fi.hel.verkkokauppa.merchant.model.MerchantModel;
import fi.hel.verkkokauppa.merchant.repository.MerchantRepository;
import fi.hel.verkkokauppa.merchant.service.MerchantService;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.NestedServletException;

import fi.hel.verkkokauppa.merchant.testing.utils.AutoMockBeanFactory;

import java.util.ArrayList;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
public class MerchantControllerUnitTest {

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

}
