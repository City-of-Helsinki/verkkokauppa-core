package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.model.Payer;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentItem;
import fi.hel.verkkokauppa.payment.repository.PayerRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentItemRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentRepository;
import fi.hel.verkkokauppa.payment.testing.BaseFunctionalTest;
import fi.hel.verkkokauppa.payment.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@RunIfProfile(profile = "local")
@Slf4j
@TestPropertySource(properties = {
        "invoice_payment_url=https://test.url"
})
public class InvoicePaymentControllerTest extends BaseFunctionalTest {
    @Autowired
    private InvoicePaymentController invoicePaymentController;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PayerRepository payerRepository;

    @Autowired
    private PaymentItemRepository paymentItemRepository;

    private ArrayList<String> toBeDeletedPaymentId = new ArrayList<>();

    @After
    public void tearDown() {
        try {
            toBeDeletedPaymentId.forEach(paymentId -> {
                log.info(paymentId);
                paymentRepository.deleteById(paymentId);
                payerRepository.deleteByPaymentId(paymentId);
                paymentItemRepository.deleteByPaymentId(paymentId);
            });
            // Clear list because all deleted
            toBeDeletedPaymentId = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
            toBeDeletedPaymentId = new ArrayList<>();
        }
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testCreatePaymentFromOrder() {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setPaymentMethod("invoice");
        OrderWrapper orderWrapper = createDummyOrderWrapper();
        paymentRequestDataDto.setOrder(orderWrapper);

        ResponseEntity<Payment> paymentResponse = invoicePaymentController.createPaymentFromOrder(paymentRequestDataDto);
        Payment payment = paymentResponse.getBody();
        assertNotNull(payment.getPaymentId());
        toBeDeletedPaymentId.add(payment.getPaymentId());
        /* Check payment */
        assertEquals(orderWrapper.getOrder().getOrderId(), payment.getOrderId());

        /* Check payment items */
        List<PaymentItem> paymentItems = paymentItemRepository.findByPaymentId(payment.getPaymentId());
        assertEquals(1, paymentItems.size());

        PaymentItem paymentItem = paymentItems.get(0);
        assertEquals(orderWrapper.getOrder().getOrderId(), paymentItem.getOrderId());
        assertEquals(orderWrapper.getItems().get(0).getProductId(), paymentItem.getProductId());

        /* Check payer */
        List<Payer> payers = payerRepository.findByPaymentId(payment.getPaymentId());
        assertEquals(1, payers.size());

        Payer payer = payers.get(0);
        assertEquals(orderWrapper.getOrder().getCustomerFirstName(), payer.getFirstName());
        assertEquals(orderWrapper.getOrder().getCustomerLastName(), payer.getLastName());
        assertEquals(orderWrapper.getOrder().getCustomerEmail(), payer.getEmail());
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testGetPaymentUrl() {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setPaymentMethod("invoice");
        OrderWrapper orderWrapper = createDummyOrderWrapper();
        paymentRequestDataDto.setOrder(orderWrapper);

        ResponseEntity<Payment> paymentResponse = invoicePaymentController.createPaymentFromOrder(paymentRequestDataDto);
        Payment payment = paymentResponse.getBody();
        assertNotNull(payment.getPaymentId());
        toBeDeletedPaymentId.add(payment.getPaymentId());

        String orderId = orderWrapper.getOrder().getOrderId();
        String userId = orderWrapper.getOrder().getUser();

        ResponseEntity<String> paymentUrlResponse = invoicePaymentController.getPaymentUrl(orderWrapper.getOrder().getNamespace(), orderId, userId);
        String paymentUrl = paymentUrlResponse.getBody();
        assertEquals(paymentUrl, "https://test.url" + "?orderId=" + orderId + "&user=" + userId);
    }
}
