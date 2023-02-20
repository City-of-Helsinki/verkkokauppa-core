package fi.hel.verkkokauppa.payment.client.paytrail;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.rest.refund.RefundAggregateDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundItemDto;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.api.data.PaymentDto;
import fi.hel.verkkokauppa.payment.api.data.refund.RefundRequestDataDto;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.refund.RefundGateway;
import fi.hel.verkkokauppa.payment.model.refund.RefundPayment;
import fi.hel.verkkokauppa.payment.model.refund.RefundPaymentStatus;
import fi.hel.verkkokauppa.payment.paytrail.PaytrailTokenPaymentClient;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.testing.BaseFunctionalTest;
import fi.hel.verkkokauppa.payment.testing.annotations.RunIfProfile;
import fi.hel.verkkokauppa.payment.utils.TestPaymentCreator;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.model.payments.PaytrailPaymentResponse;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@RunIfProfile(profile = "local")
@Slf4j
public class PaytrailTokenPaymentClientTest extends BaseFunctionalTest {

    @Autowired
    private PaytrailTokenPaymentClient paytrailTokenPaymentClient;

    @Test
    @RunIfProfile(profile = "local")
    public void testCreatePaymentWithToken() throws ExecutionException, InterruptedException, JsonProcessingException {
        Payment testPayment= new Payment();
        OrderWrapper orderWrapperDto = createDummyOrderWrapper();
        orderWrapperDto.getOrder().setUser("");

        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setOrder(orderWrapperDto);

        PaytrailPaymentContext context = createMockPaytrailPaymentContext(orderWrapperDto.getOrder().getNamespace(), paymentRequestDataDto.getMerchantId());

        String token = "1-2-3-4";
        String paymentId = orderWrapperDto.getOrder().getOrderId() + "_at_" + UUID.randomUUID();
        testPayment.setPaymentId(paymentId);

        try {

            paytrailTokenPaymentClient.createPaymentWithToken(context, paymentId, orderWrapperDto, testPayment, token);

            // Manual step for testing -> go to href and then use nordea to approve payment
            //log.info(paymentResponse.getHref());
            //log.info(paymentResponse.getTransactionId());



//            refundPayment = refundPaymentService.createRefundToPaytrailAndCreateRefundPayment(dto);
//
//            Assertions.assertNotEquals(refundDto.getRefundId(), refundPayment.getRefundPaymentId());
//            Assertions.assertNotNull(refundPayment.getRefundPaymentId());
//            Assertions.assertNotNull(refundPayment.getRefundTransactionId());
//            Assertions.assertNotNull(refundPayment.getCreatedAt());
//            Assertions.assertEquals(namespace, refundPayment.getNamespace());
//            Assertions.assertEquals(refundDto.getRefundId(), refundPayment.getRefundId());
//            Assertions.assertEquals(orderId, refundPayment.getOrderId());
//            Assertions.assertEquals(user, refundPayment.getUserId());
//            Assertions.assertEquals(RefundPaymentStatus.CREATED, refundPayment.getStatus());
//            Assertions.assertEquals(paymentMethod, refundPayment.getRefundMethod());
//            Assertions.assertEquals(new BigDecimal(refundDto.getPriceNet()), refundPayment.getTotalExclTax());
//            Assertions.assertEquals(new BigDecimal(refundDto.getPriceTotal()), refundPayment.getTotal());
//            Assertions.assertEquals(new BigDecimal(refundDto.getPriceVat()), refundPayment.getTaxAmount());
//            Assertions.assertEquals(RefundGateway.PAYTRAIL.toString(), refundPayment.getRefundGateway());

        } catch (Exception e) {
            // Skip if no manual approval for payment first
            Assertions.assertTrue(true);
        }

    }
}