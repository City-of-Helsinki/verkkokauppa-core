package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.order.api.data.accounting.CreateRefundAccountingRequestDto;
import fi.hel.verkkokauppa.order.api.data.accounting.RefundAccountingDto;
import fi.hel.verkkokauppa.order.model.accounting.RefundAccounting;
import fi.hel.verkkokauppa.order.model.accounting.RefundItemAccounting;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.constants.RefundAccountingStatusEnum;
import fi.hel.verkkokauppa.order.model.refund.RefundItem;
import fi.hel.verkkokauppa.order.repository.jpa.RefundAccountingRepository;
import fi.hel.verkkokauppa.order.repository.jpa.RefundItemAccountingRepository;
import fi.hel.verkkokauppa.order.test.utils.AccountingTestUtils;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// start local sftp server:
// verkkokauppa-core/docker compose up sftp
@RunIfProfile(profile = "local")
@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@Slf4j
public class RefundAccountingControllerTest extends AccountingTestUtils {

    @Autowired
    private RefundAccountingController refundAccountingController;

    @Autowired
    private RefundAccountingRepository refundAccountingRepository;

    @Autowired
    private RefundItemAccountingRepository refundItemAccountingRepository;

    @Test
    @RunIfProfile(profile = "local")
    public void testWithOneRefund() throws Exception {
        Refund refund1 = createTestRefund(UUID.randomUUID().toString(), "10", "5");
        RefundItem refundItem1 = createTestRefundItem(refund1);

        CreateRefundAccountingRequestDto request = createRefundAccountingRequest(refund1, refundItem1);
        refundAccountingController.createRefundAccounting(request);

        refund1 = refundRepository.findById(refund1.getRefundId()).orElseThrow();
        assertEquals(RefundAccountingStatusEnum.CREATED, refund1.getAccountingStatus(), "Refund1 accounting status should be created");

        RefundAccounting refundAccounting = refundAccountingRepository.findByRefundId(refund1.getRefundId());
        assertNotNull(refundAccounting);
        toBeDeletedRefundAccountingById.add(refundAccounting.getRefundId());

        RefundItemAccounting refundItemAccounting = refundItemAccountingRepository.findByRefundItemId(refundItem1.getRefundItemId());
        assertNotNull(refundItemAccounting);
        toBeDeletedRefundItemAccountingById.add(refundItemAccounting.getRefundItemId());

    }

    @Test
    @RunIfProfile(profile = "local")
    public void testWithTwoRefundItems() throws Exception {
        Refund refund1 = createTestRefund(UUID.randomUUID().toString(), "25", "10");
        RefundItem refundItem1 = createTestRefundItem(refund1, "15", "10", "5");
        RefundItem refundItem2 = createTestRefundItem(refund1, "20", "15", "5");

        CreateRefundAccountingRequestDto request = createRefundAccountingRequest(refund1, refundItem1);
        refundAccountingController.createRefundAccounting(request);

        refund1 = refundRepository.findById(refund1.getRefundId()).orElseThrow();
        assertEquals(RefundAccountingStatusEnum.CREATED, refund1.getAccountingStatus(), "Refund1 accounting status should be created");

        RefundAccounting refundAccounting = refundAccountingRepository.findByRefundId(refund1.getRefundId());
        assertNotNull(refundAccounting);
        toBeDeletedRefundAccountingById.add(refundAccounting.getRefundId());

        RefundItemAccounting refundItemAccounting = refundItemAccountingRepository.findByRefundItemId(refundItem1.getRefundItemId());
        assertNotNull(refundItemAccounting);
        assertEquals(refundItem1.getRowPriceTotal(), refundItemAccounting.getPriceGross());
        toBeDeletedRefundItemAccountingById.add(refundItemAccounting.getRefundItemId());

        RefundItemAccounting refundItemAccounting2 = refundItemAccountingRepository.findByRefundItemId(refundItem2.getRefundItemId());
        assertNotNull(refundItemAccounting2);
        assertEquals(refundItem2.getRowPriceTotal(), refundItemAccounting2.getPriceGross());
        toBeDeletedRefundItemAccountingById.add(refundItemAccounting2.getRefundItemId());

    }

    @Test
    @RunIfProfile(profile = "local")
    public void testWhenAccountingAlreadyExists() throws Exception {
        Refund refund1 = createTestRefund(UUID.randomUUID().toString(), "10", "5");
        RefundItem refundItem1 = createTestRefundItem(refund1);

        createTestRefundAccounting(refund1.getRefundId(), refund1.getOrderId());

        CreateRefundAccountingRequestDto request = createRefundAccountingRequest(refund1, refundItem1);
        ResponseEntity<RefundAccountingDto> response = refundAccountingController.createRefundAccounting(request);
        RefundAccountingDto refundDto = response.getBody();

        assertNull(refundDto);
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testWithoutItem() throws Exception {
        Refund refund1 = createTestRefund(UUID.randomUUID().toString(), "10", "5");
        RefundItem refundItem1 = createTestRefundItem(refund1);

        CreateRefundAccountingRequestDto request = createRefundAccountingRequest(refund1, refundItem1);

        // remove itemDtos
        request.setDtos(null);

        CommonApiException exception = assertThrows(CommonApiException.class, () -> {
            ResponseEntity<RefundAccountingDto> response = refundAccountingController.createRefundAccounting(request);
        });

        assertNotNull(exception);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertEquals("failed-to-create-refund-accounting", exception.getErrors().getErrors().get(0).getCode());
    }

}
