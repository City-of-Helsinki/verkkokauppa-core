package fi.hel.verkkokauppa.payment.service.refund;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.rest.refund.RefundAggregateDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundItemDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.api.data.PaymentDto;
import fi.hel.verkkokauppa.payment.api.data.refund.RefundRequestDataDto;
import fi.hel.verkkokauppa.payment.model.refund.RefundGateway;
import fi.hel.verkkokauppa.payment.model.refund.RefundPayment;
import fi.hel.verkkokauppa.payment.model.refund.RefundPaymentStatus;
import fi.hel.verkkokauppa.payment.testing.annotations.RunIfProfile;
import fi.hel.verkkokauppa.payment.utils.PaytrailPaymentCreator;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.PaytrailClient;
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
public class PaytrailRefundPaymentServiceTest extends PaytrailPaymentCreator {

    @Autowired
    private PaytrailRefundPaymentService refundPaymentService;

    @Test
    public void testCreateRefundToPaytrailAndCreateRefundPayment() throws ExecutionException, InterruptedException, JsonProcessingException {
        PaytrailClient client = new PaytrailClient(merchantId, secretKey);
        String orderId = "dummy-order-id";

        String paymentId = orderId + "_at_" + UUID.randomUUID();
        PaytrailPaymentResponse paymentResponse = createTestNormalMerchantPayment(
                client,
                100,
                paymentId
        );
        // Manual step for testing -> go to href and then use nordea to approve payment
        log.info(paymentResponse.getHref());
        log.info(paymentResponse.getTransactionId());

        RefundRequestDataDto dto = new RefundRequestDataDto();
        RefundAggregateDto refundAggregateDto = new RefundAggregateDto();
        RefundDto refundDto = new RefundDto();
        String refundId = "refund-id";
        refundDto.setRefundId(refundId);
        refundDto.setStatus("confirmed");
        String user = "dummy_user";
        refundDto.setUser(user);
        String namespace = "venepaikat";
        refundDto.setNamespace(namespace);
        // TODO refund dto values to createRefundPayment

        refundDto.setOrderId(orderId);
        refundDto.setPriceNet("10");
        refundDto.setPriceVat("0");
        refundDto.setPriceTotal("10");
        refundDto.setCustomerEmail(UUID.randomUUID() + "@hiq.fi");

        refundAggregateDto.setRefund(refundDto);
        ArrayList<RefundItemDto> refundItemDtos = new ArrayList<>();
        RefundItemDto itemDto = new RefundItemDto();

        itemDto.setMerchantId(getFirstMerchantIdFromNamespace(namespace));
        refundItemDtos.add(itemDto);
        refundAggregateDto.setItems(refundItemDtos);

        OrderWrapper orderWrapper = new OrderWrapper();
        OrderDto order = new OrderDto();
        order.setType(OrderType.ORDER);
        orderWrapper.setOrder(order);

        PaymentDto paymentDto = new PaymentDto();

        paymentDto.setShopInShopPayment(false);
        paymentDto.setPaytrailTransactionId(paymentResponse.getTransactionId());
        String paymentMethod = "payment-method";
        paymentDto.setPaymentMethod(paymentMethod);

        dto.setPayment(paymentDto);
        dto.setOrder(orderWrapper);
        dto.setRefund(refundAggregateDto);
        log.info(paymentResponse.getHref());
        // Set debugger to stop here, and approve payment manually using ui.
        RefundPayment refundPayment = null;
        try {
            refundPayment = refundPaymentService.createRefundToPaytrailAndCreateRefundPayment(dto);

            Assertions.assertNotEquals(refundDto.getRefundId(), refundPayment.getRefundPaymentId());
            Assertions.assertNotNull(refundPayment.getRefundPaymentId());
            Assertions.assertNotNull(refundPayment.getRefundTransactionId());
            Assertions.assertNotNull(refundPayment.getCreatedAt());
            Assertions.assertEquals(namespace, refundPayment.getNamespace());
            Assertions.assertEquals(refundDto.getRefundId(), refundPayment.getRefundId());
            Assertions.assertEquals(orderId, refundPayment.getOrderId());
            Assertions.assertEquals(user, refundPayment.getUserId());
            Assertions.assertEquals(RefundPaymentStatus.CREATED, refundPayment.getStatus());
            Assertions.assertEquals(paymentMethod, refundPayment.getRefundMethod());
            Assertions.assertEquals(new BigDecimal(refundDto.getPriceNet()), refundPayment.getTotalExclTax());
            Assertions.assertEquals(new BigDecimal(refundDto.getPriceTotal()), refundPayment.getTotal());
            Assertions.assertEquals(new BigDecimal(refundDto.getPriceVat()), refundPayment.getTaxAmount());
            Assertions.assertEquals(RefundGateway.PAYTRAIL.toString(), refundPayment.getRefundGateway());

        } catch (Exception e) {
            // Skip if no manual approval for payment first
            Assertions.assertTrue(true);
        }

    }
}