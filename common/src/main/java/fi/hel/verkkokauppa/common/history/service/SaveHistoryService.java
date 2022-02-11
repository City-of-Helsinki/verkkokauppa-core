package fi.hel.verkkokauppa.common.history.service;

import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.history.factory.HistoryFactory;
import fi.hel.verkkokauppa.common.history.util.HistoryUtil;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SaveHistoryService {

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private HistoryUtil historyUtil;

    @Autowired
    private HistoryFactory historyFactory;

    @Autowired
    private ServiceUrls serviceUrls;

    public JSONObject saveOrderMessageHistory(OrderMessage message){
        try {
            String request = historyUtil.toString(historyFactory.fromOrderMessage(message));
            return restServiceClient.makePostCall(serviceUrls.getHistoryServiceUrl() + "/history/create",request);
        } catch (Exception e) {
            log.error("saveOrderMessageHistory processing error: " + e.getMessage());
        }
        return null;
    }

    public JSONObject savePaymentMessageHistory(PaymentMessage message){
        try {
            String request = historyUtil.toString(historyFactory.fromPaymentMessage(message));
            return restServiceClient.makePostCall(serviceUrls.getHistoryServiceUrl() + "/history/create",request);
        } catch (Exception e) {
            log.info("savePaymentMessageHistory processing error: " + e.getMessage());
        }
        return null;
    }

    public JSONObject saveSubscriptionMessageHistory(SubscriptionMessage message){
        try {
            String request = historyUtil.toString(historyFactory.fromSubscriptionMessage(message));
            return restServiceClient.makePostCall(serviceUrls.getHistoryServiceUrl() + "/history/create",request);
        } catch (Exception e) {
            log.info("saveSubscriptionMessageHistory processing error: " + e.getMessage());
        }
        return null;
    }


}