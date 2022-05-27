package fi.hel.verkkokauppa.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentMethodListRequest;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.service.OfflinePaymentService;
import fi.hel.verkkokauppa.payment.testing.utils.AutoMockBeanFactory;
import fi.hel.verkkokauppa.payment.util.CurrencyUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * This class is used to test the controller layer of the application
 * <p>
 * Change OfflinePaymentController.class to controller which you want to test.
 */
@WebMvcTest(OfflinePaymentController.class) // Change and uncomment
@Import(OfflinePaymentController.class) // Change and uncomment
@ContextConfiguration(classes = AutoMockBeanFactory.class) // This automatically mocks missing beans
@AutoConfigureMockMvc // This activates auto configuration to call mocked api endpoints.
@Slf4j
@EnableAutoConfiguration(exclude = {
        ActiveMQAutoConfiguration.class,
        KafkaAutoConfiguration.class
})
public class OfflinePaymentControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    // You need to add all dependencies in controller with @Autowired annotation
    // as new field with @MockBean to controller test.
    @MockBean
    private OfflinePaymentService offlinePaymentService;

    @Test
    public void throwsError400IfNotFoundTest() throws Exception {
        GetPaymentMethodListRequest request = getGetPaymentMethodListRequest();

        OrderDto orderDto = getOrderDto();

        request.setOrderDto(orderDto);

        Exception exception = assertThrows(NestedServletException.class, () -> {
            this.mockMvc.perform(
                            post("/payment/offline/get-available-methods")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(request))
                    )
                    .andDo(print())
                    .andExpect(status().is4xxClientError());
        });

        CommonApiException cause = (CommonApiException) exception.getCause();
        assertEquals(CommonApiException.class, cause.getClass());
        assertEquals(HttpStatus.NOT_FOUND, cause.getStatus());
        assertEquals("offline-payment-methods-not-found-from-backend", cause.getErrors().getErrors().get(0).getCode());
        assertEquals("offline payment methods for namespace[venepaikat] not found from backend", cause.getErrors().getErrors().get(0).getMessage());
    }

    private OrderDto getOrderDto() {
        OrderDto orderDto = new OrderDto();
        String orderId = UUID.randomUUID().toString();
        orderDto.setOrderId(orderId);
        orderDto.setNamespace("venepaikat");
        orderDto.setUser("dummy_user");
        orderDto.setType("order");
        return orderDto;
    }

    @Test
    public void whenOfflinePaymentResultIs200Test() throws Exception {
        GetPaymentMethodListRequest request = getGetPaymentMethodListRequest();

        OrderDto orderDto = getOrderDto();
        request.setOrderDto(orderDto);

        PaymentMethodDto[] mockedReturn = {
                new PaymentMethodDto(
                "Helsinki lasku",
                "helsinki-invoice",
                "helsinki-invoice",
                "helsinki-invoice.png")
        };

        when(offlinePaymentService.getFilteredPaymentMethodList(any())).thenReturn(mockedReturn);


        this.mockMvc.perform(
                        post("/payment/offline/get-available-methods")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(mapper.writeValueAsString(mockedReturn)));

    }

    private GetPaymentMethodListRequest getGetPaymentMethodListRequest() {
        GetPaymentMethodListRequest request = new GetPaymentMethodListRequest();
        request.setNamespace("venepaikat");
        request.setCurrency(CurrencyUtil.DEFAULT_CURRENCY);
        return request;
    }

}
