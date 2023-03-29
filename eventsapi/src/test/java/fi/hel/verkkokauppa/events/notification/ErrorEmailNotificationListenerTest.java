package fi.hel.verkkokauppa.events.notification;

import fi.hel.verkkokauppa.common.configuration.QueueConfigurations;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.ErrorMessage;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.events.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
@RunIfProfile(profile = "local")
public class ErrorEmailNotificationListenerTest {

    @Autowired
    ErrorEmailNotificationListener errorEmailNotificationListener;

    @Autowired
    SendNotificationService sendNotificationService;

    @Autowired
    QueueConfigurations queueConfigurations;

    @Autowired
    private RestServiceClient restServiceClient;

    @Value("${error.notification.email:jarkko.amb@gmail.com}")
    private String receivers;

    private String mailHogUrl = "http://localhost:8025";

    @Test
    public void testErrorNotificationSending() throws InterruptedException {
        log.info("Running testErrorNotificationSending");

        ErrorMessage queueMessage = ErrorMessage.builder()
                .eventType(EventType.ERROR_EMAIL_NOTIFICATION)
                .eventTimestamp(DateTimeUtil.getDateTime())
                .message("Test error notification")
                .cause("Test cause")
                .build();

        // get number of emails before test
        JSONObject mailHogResponse;
        mailHogResponse = restServiceClient.makeGetCall(mailHogUrl + "/api/v2/messages");
        int totalMailsBefore = Integer.parseInt(mailHogResponse.get("total").toString());

        sendNotificationService.sendToQueue(queueMessage, queueConfigurations.getErrorEmailNotificationsQueue());
        sleep(3000);

        // get eMails from MailHog
        mailHogResponse = restServiceClient.makeGetCall(mailHogUrl + "/api/v2/messages");
        int totalMailsAfter = Integer.parseInt(mailHogResponse.get("total").toString());

        assertEquals("There should be one more eMail after the test.", 1, (totalMailsAfter - totalMailsBefore));

        JSONArray items = mailHogResponse.getJSONArray("items");
        // latest email is in index 0
        JSONObject email = items.getJSONObject(0);
        JSONObject headers = email.getJSONObject("Content").getJSONObject("Headers");

        assertEquals("Email Subject does not match.",
                EventType.ERROR_EMAIL_NOTIFICATION,
                headers.getJSONArray("Subject").getString(0));

        // Verify send to addresses
        String[] expectedReceivers = receivers.split(",");
        String[] actualReceivers = headers.getJSONArray("To").getString(0).split(",");

        assertEquals("Wrong number of receivers.", expectedReceivers.length, actualReceivers.length);
        for (int i = 0; i < expectedReceivers.length; i++) {
            assertEquals("Receiver address does not match.",
                    expectedReceivers[i],
                    actualReceivers[i]);
        }

        // verify sender
        assertEquals("Sender address does not match.",
                "donotreply.checkout@hel.fi",
                headers.getJSONArray("From").getString(0));

        // Verify Body
        String body = email.getJSONObject("Content").getString("Body");
        assertTrue("Body is missing Error Message.", body.contains("Test error notification"));
        assertTrue("Body is missing Error Cause.", body.contains("Test cause"));
    }
}
