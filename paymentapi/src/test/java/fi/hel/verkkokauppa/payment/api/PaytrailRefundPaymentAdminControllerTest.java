package fi.hel.verkkokauppa.payment.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.EncryptorUtil;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.payment.api.data.PaymentFilterDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.constant.PaymentGatewayEnum;
import fi.hel.verkkokauppa.payment.model.PaymentFilter;
import fi.hel.verkkokauppa.payment.model.PaymentMethodModel;
import fi.hel.verkkokauppa.payment.repository.PaymentFilterRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentMethodRepository;
import fi.hel.verkkokauppa.payment.testing.BaseFunctionalTest;
import fi.hel.verkkokauppa.payment.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RunIfProfile(profile = "local")
@SpringBootTest
@Slf4j
public class PaytrailRefundPaymentAdminControllerTest extends BaseFunctionalTest {

    @Autowired
    private PaytrailRefundPaymentAdminController paytrailRefundPaymentAdminController;

    @Test
    @RunIfProfile(profile = "local")
    public void testPaymentNotFound() {

        String code = "test-order-code";
        CommonApiException exception = assertThrows(CommonApiException.class, () -> {
            paytrailRefundPaymentAdminController.getRefundPayment(code);
        });

        assertEquals(CommonApiException.class, exception.getClass());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("failed-to-get-refund-payment", exception.getErrors().getErrors().get(0).getCode());
        assertEquals("failed to get refund payment with order id [test-order-code]", exception.getErrors().getErrors().get(0).getMessage());
    }

}
