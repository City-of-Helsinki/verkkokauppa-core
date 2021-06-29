package fi.hel.verkkokauppa.payment.service;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.payment.api.OrderPaymentDto;
import fi.hel.verkkokauppa.payment.model.Payment;

@Component
public class OnlinePaymentService {

    private Logger log = LoggerFactory.getLogger(OnlinePaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ServiceConfigurationClient serviceConfigurationClient;


    public String createFromOrder(OrderPaymentDto dto) {

        // get common payment configuration from configuration api
        WebClient client = serviceConfigurationClient.getClient();
        String namespace = dto.getNamespace();
        JSONObject namespaceServiceConfiguration = serviceConfigurationClient.getAllServiceConfiguration(client, namespace);

        // refer to ServiceConfigurationKeys
        String payment_return_url = (String) namespaceServiceConfiguration.get("payment_return_url");
        log.debug("namespace: " + namespace + " payment_return_url: " + payment_return_url);

        // TODO use the common payment configuration values

        // construct a Payment entity
        Payment payment = createPayment(dto);
        String paymentId = payment.getPaymentId();

        // TODO a redirect url containing id of created Payment
        return "https://localhost/?paymentId=" + paymentId;
    }

    private Payment createPayment(OrderPaymentDto dto) {
        String namespace = dto.getNamespace();
        String paymentId = UUIDGenerator.generateType3UUIDString(namespace, dto.getOrderId());
        Payment payment = new Payment(paymentId, namespace, dto.getOrderId(), dto.getPaymentType(), dto.getSum(), dto.getDescription());

        paymentRepository.save(payment);
        log.debug("created payment for namespace: " + namespace + " with paymentId: " + paymentId);

        return payment;
    }
    
}
