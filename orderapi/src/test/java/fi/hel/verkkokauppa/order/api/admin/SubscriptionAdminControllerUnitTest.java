package fi.hel.verkkokauppa.order.api.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionCardExpiredDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionCriteria;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionCardExpiredRepository;
import fi.hel.verkkokauppa.order.service.renewal.SubscriptionRenewalService;
import fi.hel.verkkokauppa.order.service.subscription.*;
import fi.hel.verkkokauppa.order.unit.utils.AutoMockBeanFactory;
import fi.hel.verkkokauppa.order.unit.utils.UnitTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.util.NestedServletException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UnitTest
@WebMvcTest(SubscriptionAdminController.class)
@ContextConfiguration(classes = AutoMockBeanFactory.class)
@AutoConfigureMockMvc
@Import(SubscriptionAdminController.class)
public class SubscriptionAdminControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private SubscriptionCardExpiredService service;

    @MockBean
    private SearchSubscriptionQuery searchSubscriptionQuery;

    @MockBean
    private CancelSubscriptionCommand cancelSubscriptionCommand;

    @MockBean
    private SubscriptionRenewalService renewalService;

    @MockBean
    private SubscriptionItemMetaService subscriptionItemMetaService;

    @MockBean
    private GetSubscriptionQuery getSubscriptionQuery;

    @MockBean
    private SubscriptionService subscriptionService;

    @MockBean
    private SaveHistoryService saveHistoryService;

    @MockBean
    private SubscriptionCardExpiredRepository subscriptionCardExpiredRepository;

    /**
     * It tests that when the service is called with a subscriptionId and namespace, it returns a
     * SubscriptionCardExpiredDto with the same subscriptionId and namespace
     */
    @Test
    public void createCardExpiredEmailEntityRequestShouldReturnDtoFromService() throws Exception {
        SubscriptionCardExpiredDto dto = new SubscriptionCardExpiredDto();
        String namespace = "test-namespace";
        String subscriptionId = "1";
        dto.setNamespace(namespace);
        dto.setSubscriptionId(subscriptionId);
        when(service.createAndTransformToDto(subscriptionId, namespace)).thenReturn(dto);

        this.mockMvc.perform(
                        get("/subscription-admin/create-card-expired-email-entity")
                                .param("subscriptionId", subscriptionId)
                                .param("namespace", namespace)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(mapper.writeValueAsString(dto)));
        // createAndTransformToDto should be called one time
        verify(service, times(1)).createAndTransformToDto(any(), any());

    }

    /**
     * > It checks if there are any subscriptions with expiring cards and if so, it triggers an event
     */
    @Test
    public void checkExpiringCardShouldFoundSubscriptionsWhichHaveExpiringCards() throws Exception {
        SubscriptionDto subscriptionDto = new SubscriptionDto();
        subscriptionDto.setSubscriptionId("1");
        ArrayList<SubscriptionDto> dtos = new ArrayList<>();
        dtos.add(subscriptionDto);
        // When searching active subscriptions then return mocked dto
        when(searchSubscriptionQuery.searchActive(any(SubscriptionCriteria.class))).thenReturn(dtos);
        // Mock that this subscription is expiring
        when(subscriptionService.isExpiringCard(any(LocalDate.class), any(SubscriptionDto.class))).thenReturn(true);

        this.mockMvc.perform(
                        get("/subscription-admin/check-expiring-card")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(mapper.writeValueAsString(dtos)));

        // Mock that this subscription is not expiring
        when(subscriptionService.isExpiringCard(any(LocalDate.class), any(SubscriptionDto.class))).thenReturn(false);

        ResultMatcher isEmptyList = content().string(mapper.writeValueAsString(new ArrayList<>()));
        this.mockMvc.perform(
                        get("/subscription-admin/check-expiring-card")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(isEmptyList);

        verify(subscriptionService, times(1)).triggerSubscriptionExpiredCardEvent(any());
        verify(subscriptionService, times(2)).isExpiringCard(any(), any());

    }

    @Test
    public void checkExpiringCardShouldPreventMultipleEmailsPerSubscription() throws Exception {
        SubscriptionDto subscriptionDto = new SubscriptionDto();
        subscriptionDto.setSubscriptionId("1");
        ArrayList<SubscriptionDto> dtos = new ArrayList<>();
        dtos.add(subscriptionDto);
        // When searching active subscriptions then return mocked dto
        when(searchSubscriptionQuery.searchActive(any(SubscriptionCriteria.class))).thenReturn(dtos);
        // Mock that this subscription is expiring
        when(subscriptionService.isExpiringCard(any(LocalDate.class), any(SubscriptionDto.class))).thenReturn(true);

        SubscriptionCardExpiredDto subscriptionCardExpiredSent = new SubscriptionCardExpiredDto();
        subscriptionCardExpiredSent.setSubscriptionId("1");
        ArrayList<SubscriptionCardExpiredDto> subscriptionCardExpiredDtos = new ArrayList<>();
        subscriptionCardExpiredDtos.add(subscriptionCardExpiredSent);

        when(service.findAllBySubscriptionIdOrderByCreatedAtDesc(any())).thenReturn(subscriptionCardExpiredDtos);

        ResultMatcher isEmptyList = content().string(mapper.writeValueAsString(new ArrayList<>()));
        this.mockMvc.perform(
                        get("/subscription-admin/check-expiring-card")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(isEmptyList);
        //
        verify(subscriptionService, times(0)).triggerSubscriptionExpiredCardEvent(any());
        verify(subscriptionService, times(1)).isExpiringCard(any(), any());
    }

    // Testing that if the service throws an exception, the controller will throw an exception.
    // Not so happy paths
    @Test
    public void createCardExpiredEmailEntityException404() throws Exception {
        // Error 404
        Exception exception = assertThrows(NestedServletException.class, () -> {
            CommonApiException commonApiException = new CommonApiException(HttpStatus.NOT_FOUND, new Error("404", "NOT_FOUND"));
            when(service.createAndTransformToDto(any(), any())).thenThrow(commonApiException);
            this.mockMvc.perform(
                            get("/subscription-admin/create-card-expired-email-entity")
                                    .param("subscriptionId", "1")
                                    .param("namespace", "values")
                    )
                    .andDo(print())
                    .andExpect(status().is4xxClientError());
        });
        CommonApiException cause = (CommonApiException) exception.getCause();
        assertEquals(CommonApiException.class, cause.getClass());
        assertEquals(HttpStatus.NOT_FOUND, cause.getStatus());
    }

    // Testing that if the service throws an exception, the controller will throw an exception.
    // Not so happy paths
    @Test
    public void createCardExpiredEmailEntityException500() throws Exception {
        // Error 500, internal server error
        Exception exception2 = assertThrows(NestedServletException.class, () -> {
            RuntimeException commonApiException = new RuntimeException("test");
            when(service.createAndTransformToDto(any(), any())).thenThrow(commonApiException);
            this.mockMvc.perform(
                            get("/subscription-admin/create-card-expired-email-entity")
                                    .param("subscriptionId", "1")
                                    .param("namespace", "values")
                    )
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
        });
        CommonApiException cause = (CommonApiException) exception2.getCause();
        assertEquals(CommonApiException.class, cause.getClass());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, cause.getStatus());
        assertEquals("failed-to-get-subscription", cause.getErrors().getErrors().get(0).getCode());
    }

}
