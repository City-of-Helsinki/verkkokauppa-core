package fi.hel.verkkokauppa.order.api.admin;

import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionStatus;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.order.service.renewal.SubscriptionRenewalService;
import fi.hel.verkkokauppa.order.service.subscription.SubscriptionService;
import fi.hel.verkkokauppa.order.test.utils.TestUtils;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Slf4j
@RunIfProfile(profile = "local")
class SubscriptionAdminControllerTest extends TestUtils {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionAdminController subscriptionAdminController;

    @Autowired
    private RestServiceClient restServiceClient;

    @MockBean
    private SubscriptionRenewalService renewalServiceMock;

    private String mailHogUrl = "http://localhost:8025";

    @Test
    public void assertTrue() {
        Assertions.assertTrue(true);
    }

    /**
     * If the subscription card is expiring, then the subscription status should be set to expiring.
     */
    @Test
    @RunIfProfile(profile = "local")
    void isExpiringSubscriptionCard() {
        LocalDate today = LocalDate.now();
        // Fetch subscription
        Subscription firstSubscription = createAndGetMonthlySubscription();
        SubscriptionDto subscriptionDto = subscriptionAdminController
                .getSubscription(firstSubscription.getSubscriptionId())
                .getBody();

        assert subscriptionDto != null;
        // False because subscription card information does not exists
        Assertions.assertFalse(subscriptionService.isExpiringCard(LocalDate.now(), subscriptionDto));

        // Add correct card information
        firstSubscription.setPaymentMethodExpirationMonth((byte) today.getMonth().getValue());
        firstSubscription.setPaymentMethodExpirationYear((short) today.getYear());

        subscriptionRepository.save(firstSubscription);

        SubscriptionDto refetchedDto = subscriptionAdminController
                .getSubscription(firstSubscription.getSubscriptionId())
                .getBody();
        assert refetchedDto != null;
        Assertions.assertFalse(subscriptionService.isExpiringCard(LocalDate.now(), refetchedDto));

        // Add expiring card information
        LocalDate todayMinusOneMonth = today.minus(1, ChronoUnit.MONTHS);
        firstSubscription.setPaymentMethodExpirationMonth((byte) todayMinusOneMonth.getMonth().getValue());
        firstSubscription.setPaymentMethodExpirationYear((short) todayMinusOneMonth.getYear());

        subscriptionRepository.save(firstSubscription);
        SubscriptionDto expiredCardSubscriptionDto = subscriptionAdminController
                .getSubscription(firstSubscription.getSubscriptionId())
                .getBody();
        assert expiredCardSubscriptionDto != null;
        Assertions.assertTrue(subscriptionService.isExpiringCard(LocalDate.now(), expiredCardSubscriptionDto));

        firstSubscription.setEndDate(LocalDateTime.now().minus(1, ChronoUnit.MONTHS));
        subscriptionRepository.save(firstSubscription);
        Assertions.assertEquals(SubscriptionStatus.ACTIVE, firstSubscription.getStatus());

    }

    /**
     * > This function tests the endpoint that returns all subscriptions with expiring cards
     */
    @Test
    @RunIfProfile(profile = "local")
    void isExpiringSubscriptionCardEndpoint() {
        LocalDate today = LocalDate.now();
        // Fetch subscription
        Subscription firstSubscription = createAndGetMonthlySubscription();
        SubscriptionDto subscriptionDto = subscriptionAdminController
                .getSubscription(firstSubscription.getSubscriptionId())
                .getBody();

        assert subscriptionDto != null;
        // False because subscription card information does not exists
        Assertions.assertFalse(subscriptionService.isExpiringCard(LocalDate.now(), subscriptionDto));

        // Add expiring card information
        LocalDate todayMinusOneMonth = today.minus(1, ChronoUnit.MONTHS);
        firstSubscription.setPaymentMethodExpirationMonth((byte) todayMinusOneMonth.getMonth().getValue());
        firstSubscription.setPaymentMethodExpirationYear((short) todayMinusOneMonth.getYear());
        firstSubscription.setEndDate(LocalDateTime.now().minus(1, ChronoUnit.DAYS));

        subscriptionRepository.save(firstSubscription);
        SubscriptionDto expiredCardSubscriptionDto = subscriptionAdminController
                .getSubscription(firstSubscription.getSubscriptionId())
                .getBody();
        assert expiredCardSubscriptionDto != null;
        Assertions.assertTrue(subscriptionService.isExpiringCard(LocalDate.now(), expiredCardSubscriptionDto));

        List<SubscriptionDto> dtos = subscriptionAdminController.getSubscriptionsWithExpiringCard();

        SubscriptionDto filteredOne = dtos.stream().filter(subscriptionDto1 ->
                        Objects.equals(
                                subscriptionDto1.getSubscriptionId(),
                                firstSubscription.getSubscriptionId()))
                .collect(Collectors.toList()).get(0);
        Assertions.assertNotNull(filteredOne);
    }

    @Test
    @RunIfProfile(profile = "local")
    void checkRenewalsWhenRenewalsExist() throws InterruptedException {
        ReflectionTestUtils.setField(subscriptionAdminController, "renewalService", renewalServiceMock);
        when(renewalServiceMock.renewalRequestsExist()).thenReturn(true);

        // get number of emails before test
        JSONObject mailHogResponse;
        mailHogResponse = restServiceClient.makeGetCall(mailHogUrl + "/api/v2/messages");
        int totalMailsBefore = Integer.parseInt(mailHogResponse.get("total").toString());

        // generate checkRenewals error notification
        subscriptionAdminController.checkRenewals();
        sleep(3000);

        // get eMails from MailHog
        mailHogResponse = restServiceClient.makeGetCall(mailHogUrl + "/api/v2/messages");
        int totalMailsAfter = Integer.parseInt(mailHogResponse.get("total").toString());
        assertEquals("There should be one more eMail after the test.", 1, (totalMailsAfter - totalMailsBefore));

        JSONArray items = mailHogResponse.getJSONArray("items");
        // latest email is in index 0
        JSONObject email = items.getJSONObject(0);
        JSONObject headers = email.getJSONObject("Content").getJSONObject("Headers");

        // remove the test email
        restServiceClient.makeDeleteCall(mailHogUrl + "/api/v1/messages/" + email.getString("ID"));

        assertEquals("Email Subject does not match.",
                EventType.ERROR_EMAIL_NOTIFICATION,
                headers.getJSONArray("Subject").getString(0));

        // Verify Body
        String body = email.getJSONObject("Content").getString("Body");
        Assertions.assertTrue(
                body.contains("Endpoint: /subscription-admin/check-renewals.")
        );
        Assertions.assertTrue(
                body.contains("checkRenevals (subscription) called before previous reneval requests were handled.")
        );
    }

    @Test
    @RunIfProfile(profile = "local")
    void testStartProcessingRenewalsErrorNotification() throws InterruptedException {
        ReflectionTestUtils.setField(subscriptionAdminController, "renewalService", renewalServiceMock);
        when(renewalServiceMock.renewalRequestsExist()).thenReturn(true).thenReturn(false);

        // get number of emails before test
        JSONObject mailHogResponse;
        mailHogResponse = restServiceClient.makeGetCall(mailHogUrl + "/api/v2/messages");
        int totalMailsBefore = Integer.parseInt(mailHogResponse.get("total").toString());

        // set interrupted flag for thread
        Thread.currentThread().interrupt();
        subscriptionAdminController.startProcessingRenewals();
        sleep(3000);

        // get eMails from MailHog
        mailHogResponse = restServiceClient.makeGetCall(mailHogUrl + "/api/v2/messages");
        int totalMailsAfter = Integer.parseInt(mailHogResponse.get("total").toString());
        assertEquals("There should be one more eMail after the test.", 1, (totalMailsAfter - totalMailsBefore));

        JSONArray items = mailHogResponse.getJSONArray("items");
        // latest email is in index 0
        JSONObject email = items.getJSONObject(0);
        JSONObject headers = email.getJSONObject("Content").getJSONObject("Headers");

        // remove the test email
        restServiceClient.makeDeleteCall(mailHogUrl + "/api/v1/messages/" + email.getString("ID"));

        assertEquals("Email Subject does not match.",
                EventType.ERROR_EMAIL_NOTIFICATION,
                headers.getJSONArray("Subject").getString(0));

        // Verify Body
        String body = email.getJSONObject("Content").getString("Body");
        Assertions.assertTrue(
                body.contains("Endpoint: /subscription-admin/start-processing-renewals.")
        );
    }
}