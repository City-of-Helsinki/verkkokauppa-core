package fi.hel.verkkokauppa.events.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.io.IOException;

@Slf4j
public abstract class BaseEmailNotifier<T> {

    private static final String EMAIL_TEMPLATE_PATH = "email/template_email_notification.html";

    private final RestServiceClient restServiceClient;
    protected final ServiceUrls serviceUrls;
    protected final ObjectMapper mapper;

    protected BaseEmailNotifier(
            RestServiceClient restServiceClient,
            ServiceUrls serviceUrls,
            ObjectMapper mapper
    ) {
        this.restServiceClient = restServiceClient;
        this.serviceUrls = serviceUrls;
        this.mapper = mapper;
    }

    protected void sendNotificationToEmail(String id, String receiver, String header, String generalInfo, String callstack, T eventPayload) throws IOException {
        JSONObject emailMsgData = createEmailMessageJson(id, receiver, header, generalInfo, callstack, eventPayload);
        restServiceClient.makePostCall(serviceUrls.getMessageServiceUrl() + "/message/send/email", emailMsgData.toString());
    }

    private JSONObject createEmailMessageJson(String id, String receiver, String header, String generalInfo, String callstack, T eventPayload) throws IOException {
        JSONObject msgJson = new JSONObject();
        msgJson.put("id", id);
        msgJson.put("receiver", receiver);
        msgJson.put("header", header);
        log.info("Initial mail message without body: {}", msgJson.toString());

        String html = FileUtils.readFileAsString(EMAIL_TEMPLATE_PATH, getClass().getClassLoader());
        html = html.replace("#EVENT_TYPE#", header);
        html = html.replace("#GENERAL_INFORMATION#", "<p>" + generalInfo + "</p>");
        html = html.replace("#CALLSTACK#", "<p>" + callstack + "</p>");
        html = html.replace("#EVENT_PAYLOAD#", mapper.writeValueAsString(eventPayload));

        msgJson.put("body", html);

        return msgJson;
    }

    protected Object getObjectFromTextMessage(TextMessage textMessage, Class<T> objectClass) throws JMSException, JsonProcessingException {
        String jsonMessage = textMessage.getText();
        log.info(ErrorEmailNotificationListener.class + "{}", jsonMessage);

        return mapper.readValue(jsonMessage, objectClass);
    }
}
