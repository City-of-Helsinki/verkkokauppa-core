package fi.hel.verkkokauppa.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.payment.api.data.PaymentFilterDto;
import fi.hel.verkkokauppa.payment.logic.fetcher.CancelPaymentFetcher;
import fi.hel.verkkokauppa.payment.logic.validation.PaymentReturnValidator;
import fi.hel.verkkokauppa.payment.model.PaymentFilter;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.service.PaymentFilterService;
import fi.hel.verkkokauppa.payment.testing.utils.AutoMockBeanFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
/**
 * This class is used to test the controller layer of the application
 * <p>
 * Change OfflinePaymentController.class to controller which you want to test.
 */
@WebMvcTest(PaymentAdminController.class) // Change and uncomment
@Import(PaymentAdminController.class) // Change and uncomment
@ContextConfiguration(classes = {AutoMockBeanFactory.class, ValidationAutoConfiguration.class})
// This automatically mocks missing beans
@AutoConfigureMockMvc // This activates auto configuration to call mocked api endpoints.
@Slf4j
@EnableAutoConfiguration(exclude = {
        ActiveMQAutoConfiguration.class,
        KafkaAutoConfiguration.class
})
public class PaymentAdminControllerUnitTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private PaymentAdminController paymentAdminController;
    @MockBean
    private OnlinePaymentService service;
    @MockBean
    private CancelPaymentFetcher cancelPaymentFetcher;
    @MockBean
    private PaymentReturnValidator paymentReturnValidator;
    @MockBean
    private SaveHistoryService saveHistoryService;
    @MockBean
    private PaymentFilterService filterService;
    @Test
    public void savePaymentFilter() throws Exception {
        List<PaymentFilterDto> request = new ArrayList<>();
        PaymentFilterDto paymentFilterDto = new PaymentFilterDto();
        paymentFilterDto.setReferenceId("setReferenceId");
        paymentFilterDto.setType("setType");
        paymentFilterDto.setValue("setValue");
        request.add(paymentFilterDto);

        List<PaymentFilter> responseFilters = new ArrayList<>();
        PaymentFilter paymentFilter = mapper.convertValue(paymentFilterDto, PaymentFilter.class);
        String filterId = "123";
        paymentFilter.setFilterId(filterId);
        responseFilters.add(paymentFilter);
        when(filterService.savePaymentFilters(any())).thenReturn(responseFilters);

        ReflectionTestUtils.setField(filterService, "objectMapper", mapper);
        when(filterService.mapPaymentFilterListToDtoList(any())).thenCallRealMethod();

        ResponseEntity<List<PaymentFilterDto>> response = paymentAdminController.savePaymentFilters(request);

        Assertions.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
        PaymentFilterDto expected = Objects.requireNonNull(response.getBody()).get(0);
        Assertions.assertNotNull(expected.getFilterId());
        Assertions.assertEquals(expected.getReferenceId(), paymentFilterDto.getReferenceId());
        Assertions.assertEquals(expected.getType(), paymentFilterDto.getType());
        Assertions.assertEquals(expected.getValue(), paymentFilterDto.getValue());
    }
}