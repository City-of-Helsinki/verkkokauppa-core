package fi.hel.verkkokauppa.order.api.data.dto;

import fi.hel.verkkokauppa.common.rest.refund.RefundItemDto;
import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.api.data.OrderDto;
import fi.hel.verkkokauppa.order.api.data.accounting.ProductAccountingDto;
import fi.hel.verkkokauppa.order.api.data.accounting.RefundItemAccountingDto;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.model.refund.RefundItem;
import fi.hel.verkkokauppa.order.service.CommonBeanValidationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.validation.*;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class )
@SpringBootTest
public class RefundItemAccountingDtoTests extends DummyData {

    @Autowired
    private CommonBeanValidationService commonBeanValidationService;

    @Before
    public void setUp() {

    }

    @Test
    public void identicalRefundItemDtos() {
        Refund refund = generateDummyRefund("1");
        ProductAccountingDto productAccountingDto = getDummyProductAccountingDto();

        RefundItem refundItem1 = generateDummyRefundItem(refund);
        RefundItem refundItem2 = generateDummyRefundItem(refund);

        RefundItemAccountingDto refundItemAccountingDto1 = new RefundItemAccountingDto(refundItem1, productAccountingDto);
        RefundItemAccountingDto refundItemAccountingDto2 = new RefundItemAccountingDto(refundItem2, productAccountingDto);

        assertTrue(refundItemAccountingDto1.equals(refundItemAccountingDto2));
    }

    @Test
    public void sameRefundItemDto() {
        Refund refund = generateDummyRefund("1");
        ProductAccountingDto productAccountingDto = getDummyProductAccountingDto();

        RefundItem refundItem1 = generateDummyRefundItem(refund);

        RefundItemAccountingDto refundItemAccountingDto1 = new RefundItemAccountingDto(refundItem1, productAccountingDto);

        assertTrue(refundItemAccountingDto1.equals(refundItemAccountingDto1));
    }

    @Test
    public void differentObjectTypeRefundItemDto() {
        Refund refund = generateDummyRefund("1");
        ProductAccountingDto productAccountingDto = getDummyProductAccountingDto();

        RefundItem refundItem1 = generateDummyRefundItem(refund);

        RefundItemAccountingDto refundItemAccountingDto1 = new RefundItemAccountingDto(refundItem1, productAccountingDto);

        assertFalse(refundItemAccountingDto1.equals(refundItem1));
        assertFalse(refundItemAccountingDto1.equals(null));
    }

    @Test
    public void differentRefundItemDtos() {
        Refund refund = generateDummyRefund("1");
        ProductAccountingDto productAccountingDto = getDummyProductAccountingDto();

        RefundItem refundItem1 = generateDummyRefundItem(refund);
        RefundItem refundItem2 = generateDummyRefundItem(refund);

        RefundItemAccountingDto refundItemAccountingDto1 = new RefundItemAccountingDto(refundItem1, productAccountingDto);

        productAccountingDto.setBalanceProfitCenter("otherBalanceProfitCenter");
        RefundItemAccountingDto refundItemAccountingDto2 = new RefundItemAccountingDto(refundItem2, productAccountingDto);
        assertFalse(refundItemAccountingDto1.equals(refundItemAccountingDto2));

        productAccountingDto = getDummyProductAccountingDto();
        productAccountingDto.setMainLedgerAccount("otherMainLedgerAccount");
        refundItemAccountingDto2 = new RefundItemAccountingDto(refundItem2, productAccountingDto);
        assertFalse(refundItemAccountingDto1.equals(refundItemAccountingDto2));

        productAccountingDto = getDummyProductAccountingDto();
        productAccountingDto.setVatCode("otherVatCode");
        refundItemAccountingDto2 = new RefundItemAccountingDto(refundItem2, productAccountingDto);
        assertFalse(refundItemAccountingDto1.equals(refundItemAccountingDto2));

        productAccountingDto = getDummyProductAccountingDto();
        productAccountingDto.setInternalOrder("otherInternalOrder");
        refundItemAccountingDto2 = new RefundItemAccountingDto(refundItem2, productAccountingDto);
        assertFalse(refundItemAccountingDto1.equals(refundItemAccountingDto2));

        productAccountingDto = getDummyProductAccountingDto();
        productAccountingDto.setProfitCenter("otherProfitCenter");
        refundItemAccountingDto2 = new RefundItemAccountingDto(refundItem2, productAccountingDto);
        assertFalse(refundItemAccountingDto1.equals(refundItemAccountingDto2));

        productAccountingDto = getDummyProductAccountingDto();
        productAccountingDto.setProject("otherProject");
        refundItemAccountingDto2 = new RefundItemAccountingDto(refundItem2, productAccountingDto);
        assertFalse(refundItemAccountingDto1.equals(refundItemAccountingDto2));

        productAccountingDto = getDummyProductAccountingDto();
        productAccountingDto.setOperationArea("otherOperationArea");
        refundItemAccountingDto2 = new RefundItemAccountingDto(refundItem2, productAccountingDto);
        assertFalse(refundItemAccountingDto1.equals(refundItemAccountingDto2));
    }

    private ProductAccountingDto getDummyProductAccountingDto(){
        ProductAccountingDto productAccountingDto = new ProductAccountingDto();
        productAccountingDto.setVatCode("vatCode");
        productAccountingDto.setCompanyCode("companyCode");
        productAccountingDto.setMainLedgerAccount("mainLedgerAccount");
        productAccountingDto.setInternalOrder("internalOrder");
        productAccountingDto.setProfitCenter("profitCenter");
        productAccountingDto.setBalanceProfitCenter("balanceProfitCenter");
        productAccountingDto.setProject("project");
        productAccountingDto.setOperationArea("operationArea");
        return productAccountingDto;
    }

}
