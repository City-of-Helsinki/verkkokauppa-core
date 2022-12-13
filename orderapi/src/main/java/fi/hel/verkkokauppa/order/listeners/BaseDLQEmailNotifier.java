package fi.hel.verkkokauppa.order.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.io.IOException;

@Slf4j
public abstract class BaseDLQEmailNotifier<T> {

    private static final String EMAIL_TEMPLATE_PATH = "email/template_email_dlq_alert.html";

    private final RestServiceClient restServiceClient;
    protected final ServiceUrls serviceUrls;
    protected final ObjectMapper mapper;

    protected BaseDLQEmailNotifier(
            RestServiceClient restServiceClient,
            ServiceUrls serviceUrls,
            ObjectMapper mapper
    ) {
        this.restServiceClient = restServiceClient;
        this.serviceUrls = serviceUrls;
        this.mapper = mapper;
    }

    protected void sendNotificationToEmail(String id, String reciever, String header, String generalInfo, String namespace, T eventPayload) throws IOException {
        JSONObject emailMsgData = createEmailMessageJson(id, reciever, header, generalInfo, namespace, eventPayload);
        restServiceClient.makePostCall(serviceUrls.getMessageServiceUrl() + "/message/send/email", emailMsgData.toString());
    }

    private JSONObject createEmailMessageJson(String id, String reciever, String header, String generalInfo, String namespace, T eventPayload) throws IOException {
        JSONObject msgJson = new JSONObject();
        msgJson.put("id", id);
        msgJson.put("receiver", reciever);
        msgJson.put("header", "DLQ queue alert - " + header);
        log.info("Initial mail message without body: {}", msgJson.toString());

        String html = FileUtils.readFileAsString(EMAIL_TEMPLATE_PATH, getClass().getClassLoader());
        html = html.replace("#EVENT_TYPE#", header);
        html = html.replace("#GENERAL_INFORMATION#", "<p>" + generalInfo + "</p>" );
        html = html.replace("#NAMESPACE#", namespace);
        html = html.replace("#EVENT_PAYLOAD#", mapper.writeValueAsString(eventPayload));

        msgJson.put("body", html);

        return msgJson;
    }
}
