package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.payment.api.data.GetPaymentMethodListRequest;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.constant.PaymentGatewayEnum;
import fi.hel.verkkokauppa.payment.model.PaymentMethodModel;
import fi.hel.verkkokauppa.payment.repository.PaymentMethodRepository;
import fi.hel.verkkokauppa.payment.testing.annotations.RunIfProfile;
import fi.hel.verkkokauppa.payment.util.CurrencyUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@EnableAutoConfiguration(exclude = {
        ActiveMQAutoConfiguration.class,
        KafkaAutoConfiguration.class
})
@Slf4j
class OfflinePaymentControllerTest {

    @Autowired
    private OfflinePaymentController offlinePaymentController;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;
    private ArrayList<String> paymentMethodsToBeDeleted = new ArrayList<>();

    @AfterEach
    void tearDown() {
        try {
            paymentMethodsToBeDeleted.forEach(s -> paymentMethodRepository.deleteByCode(s));
            // Clear list because all merchants deleted
            paymentMethodsToBeDeleted = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
        }
    }

    private PaymentMethodModel createTestPaymentMethod(PaymentGatewayEnum paymentGatewayEnum, Boolean deleteAfterTest) {
        PaymentMethodModel paymentMethodModel = new PaymentMethodModel();
        paymentMethodModel.setName("Helsinki lasku");
        paymentMethodModel.setCode("invoice-helsinki");
        paymentMethodModel.setGroup("invoice");
        paymentMethodModel.setImg("https://www.sttinfo.fi/data/images/00702/46c30821-4a5a-4050-afb9-5b9e0c29d547-w_300_h_100.png");
        paymentMethodModel.setGateway(paymentGatewayEnum);
        PaymentMethodModel saved = paymentMethodRepository.save(paymentMethodModel);
        if (deleteAfterTest) {
            paymentMethodsToBeDeleted.add(saved.getCode());
        }
        return saved;
    }

    @Test
    @RunIfProfile(profile = "local")
    void getAvailableMethods() {
        boolean deleteAfterTest = false;
        PaymentMethodModel paymentMethodModel = createTestPaymentMethod(PaymentGatewayEnum.INVOICE, deleteAfterTest);
        GetPaymentMethodListRequest request = new GetPaymentMethodListRequest();
        request.setNamespace("venepaikat");
        request.setCurrency(CurrencyUtil.DEFAULT_CURRENCY);

        OrderDto orderDto = new OrderDto();
        String orderId = UUID.randomUUID().toString();
        orderDto.setOrderId(orderId);
        orderDto.setNamespace("venepaikat");
        orderDto.setUser("dummy_user");
        orderDto.setType("order");

        request.setOrderDto(orderDto);

        ResponseEntity<PaymentMethodDto[]> response = offlinePaymentController.getAvailableMethods(request);
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().length);

        PaymentMethodDto actual = response.getBody()[0];

        assertEquals(paymentMethodModel.getName(), actual.getName());
        assertEquals(paymentMethodModel.getCode(), actual.getCode());
        assertEquals(paymentMethodModel.getGroup(), actual.getGroup());
        assertEquals(paymentMethodModel.getImg(), actual.getImg());
    }
}