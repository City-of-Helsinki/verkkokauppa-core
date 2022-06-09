package fi.hel.verkkokauppa.merchant.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.merchant.api.dto.MerchantDto;
import fi.hel.verkkokauppa.merchant.model.ConfigurationModel;
import fi.hel.verkkokauppa.merchant.repository.MerchantRepository;
import fi.hel.verkkokauppa.merchant.service.MerchantService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import fi.hel.verkkokauppa.merchant.testing.utils.AutoMockBeanFactory;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
public class MerchantControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    // You need to add all dependencies in controller with @Autowired annotation
    // as new field with @MockBean to controller test.
    @MockBean
    private MerchantService merchantService;

    @MockBean
    private MerchantRepository merchantRepository;

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

        Exception exception = assertThrows(NestedServletException.class, () -> {
            this.mockMvc.perform(
                            post("/merchant/upsert")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(merchantDto))
                    )
                    .andDo(print())
                    .andExpect(status().is5xxServerError());
        });

        CommonApiException cause = (CommonApiException) exception.getCause();
        assertEquals(CommonApiException.class, cause.getClass());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, cause.getStatus());
        assertEquals("failed-to-upsert-merchant", cause.getErrors().getErrors().get(0).getCode());
        assertEquals("failed to upsert merchant, merchantId:merchantId", cause.getErrors().getErrors().get(0).getMessage());
    }


    @Test
    public void throwsError500IfCantBeUpdatedTest() throws Exception {
        // TODO  WIP!
        MerchantDto merchantDto = new MerchantDto();
        merchantDto.setMerchantId("merchantId");
        merchantDto.setNamespace("namespace");

        Mockito.when(merchantService.update(merchantDto)).thenCallRealMethod();

        ConfigurationModel configurationModel = new ConfigurationModel();

        ArrayList<ConfigurationModel> configurationModels = new ArrayList<>();
        configurationModels.add(configurationModel);

        merchantDto.setConfigurations(configurationModels);

        Exception exception = assertThrows(NestedServletException.class, () -> {
            this.mockMvc.perform(
                            post("/merchant/upsert")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(merchantDto))
                    )
                    .andDo(print())
                    .andExpect(status().is5xxServerError());
        });

        CommonApiException cause = (CommonApiException) exception.getCause();
        assertEquals(CommonApiException.class, cause.getClass());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, cause.getStatus());
        assertEquals("failed-to-upsert-merchant", cause.getErrors().getErrors().get(0).getCode());
        assertEquals("failed to upsert merchant, merchantId:merchantId", cause.getErrors().getErrors().get(0).getMessage());
    }

}
