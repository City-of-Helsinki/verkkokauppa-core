package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.order.api.data.accounting.AccountingExportDataDto;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingSlipDto;
import fi.hel.verkkokauppa.order.constants.RefundAccountingStatusEnum;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.test.utils.AccountingTestUtils;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// start local sftp server:
// verkkokauppa-core/docker compose up sftp
@RunIfProfile(profile = "local")
@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@Slf4j
public class AccountingExportControllerTest extends AccountingTestUtils {

    @Autowired
    private AccountingController accountingController;

    @Autowired
    private AccountingExportController accountingExportController;

    @Test
    @RunIfProfile(profile = "local")
    public void testAccountingCreate() throws Exception {
        Order order1 = createTestOrder();
        Order order2 = createTestOrder();
        Order order3 = createTestOrder();

        createTestOrderAccounting(order1.getOrderId());
        createTestOrderAccounting(order2.getOrderId());

        Refund refund1 = createTestRefund(order1.getOrderId());
        setTestRefundAccountingStatus(refund1.getRefundId(), RefundAccountingStatusEnum.CREATED);

        Refund refund2 = createTestRefund(order2.getOrderId());

        createTestRefundAccounting(refund1.getRefundId(), refund1.getOrderId());

        String companyCode1 = "1234";
        createTestOrderItemAccounting(
                order1.getOrderId(),
                "10", "7", "3",
                companyCode1,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter",
                "project 1",
                "Area A"
        );
        createTestOrderItemAccounting(
                order1.getOrderId(),
                "20", "10", "10",
                companyCode1,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter2",
                "project 2",
                "Area B"
        );
        createTestOrderItemAccounting(
                order1.getOrderId(),
                "30", "20", "10",
                companyCode1,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter2",
                "project 2",
                "Area B"
        );
        createTestOrderItemAccounting(
                order1.getOrderId(),
                "50", "35", "15",
                companyCode1,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter2",
                "project 3",
                "Area A"
        );
        // add refunds
        createTestRefundItemAccounting(
                refund1.getRefundId(),
                refund1.getOrderId(),
                "-10", "-7", "-3",
                companyCode1,
                "account",
                "24",
                "yes",
                "profitCenter",
                "refundBalanceProfitCenter",
                "project 1",
                "Area A"
        );
        createTestRefundItemAccounting(
                refund1.getRefundId(),
                refund1.getOrderId(),
                "-15", "-10", "-5",
                companyCode1,
                "account",
                "24",
                "yes",
                "profitCenter",
                "refundBalanceProfitCenter2",
                "project 3",
                "Area A"
        );
        createTestRefundItemAccounting(
                refund1.getRefundId(),
                refund1.getOrderId(),
                "-25", "-15", "-10",
                companyCode1,
                "account",
                "24",
                "yes",
                "profitCenter",
                "refundBalanceProfitCenter2",
                "project 3",
                "Area A"
        );
        //
        // second company code
        //
        String companyCode2 = "5678";
        createTestOrderItemAccounting(
                order2.getOrderId(),
                "10", "5", "5",
                companyCode2,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter3",
                "project 2",
                "Area B"
        );
        createTestOrderItemAccounting(
                order2.getOrderId(),
                "10", "5", "5",
                companyCode2,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter3",
                "project 2",
                "Area B"
        );

        ResponseEntity<List<AccountingSlipDto>> response = accountingController.createAccountingData();
        String slipId = response.getBody().get(0).getAccountingSlipId();
        ResponseEntity<AccountingExportDataDto> response2 = accountingExportController.generateAccountingExportData(slipId);

        AccountingExportDataDto accountingExportDataDto = response2.getBody();
        String accountingXml = accountingExportDataDto.getXml();

        assertNotNull(accountingXml);
        assertTrue(accountingXml.contains("<ProfitCenter>profitCenter</ProfitCenter>"));
        assertTrue(accountingXml.contains("<ProfitCenter/>"));
        assertTrue(accountingXml.contains("<ProfitCenter>balanceProfitCenter</ProfitCenter>"));
        assertTrue(accountingXml.contains("<ProfitCenter>balanceProfitCenter2</ProfitCenter>"));
        assertTrue(accountingXml.contains("<ProfitCenter>refundBalanceProfitCenter</ProfitCenter>"));
        assertTrue(accountingXml.contains("<ProfitCenter>refundBalanceProfitCenter2</ProfitCenter>"));
        assertEquals(slipId, accountingExportDataDto.getAccountingSlipId());

        // add another refund accounting, refund created earlier
        setTestRefundAccountingStatus(refund2.getRefundId(), RefundAccountingStatusEnum.CREATED);
        createTestRefundAccounting(refund2.getRefundId(), refund2.getOrderId());
        createTestRefundItemAccounting(
                refund2.getRefundId(),
                refund2.getOrderId(),
                "-10", "-5", "-5",
                companyCode1,
                "account",
                "24",
                "yes",
                "profitCenter",
                "refundBalanceProfitCenter3",
                "project 2",
                "Area B"
        );

        // add another order accounting, order created earlier
        createTestOrderAccounting(order3.getOrderId());
        createTestOrderItemAccounting(
                refund2.getOrderId(),
                "10", "5", "5",
                companyCode1,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter",
                "project 2",
                "Area B"
        );

        ResponseEntity<List<AccountingSlipDto>> response3 = accountingController.createAccountingData();
        String slipId2 = response3.getBody().get(0).getAccountingSlipId();
        ResponseEntity<AccountingExportDataDto> response4 = accountingExportController.generateAccountingExportData(slipId2);

        AccountingExportDataDto accountingExportDataDto2 = response4.getBody();
        String accountingXml2 = accountingExportDataDto2.getXml();

        assertNotNull(accountingXml2);
        assertTrue(accountingXml2.contains("<ProfitCenter>profitCenter</ProfitCenter>"));
        assertTrue(accountingXml2.contains("<ProfitCenter/>"));
        assertTrue(accountingXml2.contains("<ProfitCenter>balanceProfitCenter</ProfitCenter>"));
        assertTrue(accountingXml2.contains("<ProfitCenter>refundBalanceProfitCenter3</ProfitCenter>"));
        assertEquals(slipId2, accountingExportDataDto2.getAccountingSlipId());

    }
}
