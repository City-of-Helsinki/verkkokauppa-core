package fi.hel.verkkokauppa.configuration.api.namespace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.configuration.api.merchant.dto.MerchantDto;
import fi.hel.verkkokauppa.configuration.api.namespace.dto.NamespaceDto;
import fi.hel.verkkokauppa.configuration.mapper.MerchantMapper;
import fi.hel.verkkokauppa.configuration.mapper.NamespaceMapper;
import fi.hel.verkkokauppa.configuration.model.ConfigurationModel;
import fi.hel.verkkokauppa.configuration.model.LocaleModel;
import fi.hel.verkkokauppa.configuration.model.merchant.MerchantModel;
import fi.hel.verkkokauppa.configuration.repository.MerchantRepository;
import fi.hel.verkkokauppa.configuration.repository.NamespaceRepository;
import fi.hel.verkkokauppa.configuration.service.MerchantService;
import fi.hel.verkkokauppa.configuration.service.NamespaceService;
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
@WebMvcTest(NamespaceController.class) // Change and uncomment
@Import(NamespaceController.class) // Change and uncomment
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
    private NamespaceService namespaceService;

    @MockBean
    private MerchantService merchantService;

    @MockBean
    private NamespaceRepository namespaceRepository;

    @MockBean
    private NamespaceMapper namespaceMapper;

    @Test
    public void namespaceGetKeys() throws Exception {

        List<String> allKeys = new ArrayList<>(new ArrayList<>() {{
            add("merchantOrderWebhookUrl");
            add("merchantPaymentWebhookUrl");
            add("merchantRefundWebhookUrl");
            add("merchantSubscriptionWebhookUrl");
            add("merchantTermsOfServiceUrl");
            add("orderCancelRedirectUrl");
            add("orderRightOfPurchaseIsActive");
            add("orderRightOfPurchaseUrl");
            add("orderSuccessRedirectUrl");
            add("refundSuccessRedirectUrl");    
            add("subscriptionPriceUrl");
            add("subscriptionResolveProductUrl");
        }});


        MvcResult response = this.mockMvc.perform(
                        get("/namespace/keys")
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Assertions.assertEquals(objectMapper.writeValueAsString(allKeys), response.getResponse().getContentAsString());
    }


}
