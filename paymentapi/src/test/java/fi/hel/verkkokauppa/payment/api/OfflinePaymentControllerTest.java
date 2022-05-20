package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.payment.api.data.GetPaymentMethodListRequest;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.service.OfflinePaymentService;
import fi.hel.verkkokauppa.payment.testing.annotations.RunIfProfile;
import fi.hel.verkkokauppa.payment.util.CurrencyUtil;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
class OfflinePaymentControllerTest {

    @Autowired
    private OfflinePaymentController offlinePaymentController;
    @Autowired
    private OfflinePaymentService offlinePaymentService;

    @Test
    @RunIfProfile(profile = "local")
    void getAvailableMethods() {
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

        PaymentMethodDto expected = new PaymentMethodDto(
                "Helsinki lasku",
                "helsinki-invoice",
                "helsinki-invoice",
                "helsinki-invoice.png"
        );
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getCode(), actual.getCode());
        assertEquals(expected.getGroup(), actual.getGroup());
        assertEquals(expected.getImg(), actual.getImg());
    }
}