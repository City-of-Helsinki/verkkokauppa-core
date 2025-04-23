package fi.hel.verkkokauppa.message.service;

import fi.hel.verkkokauppa.message.dto.GenerateOrderConfirmationPDFRequestDto;
import fi.hel.verkkokauppa.message.dto.OrderItemDto;
import fi.hel.verkkokauppa.message.dto.OrderItemMetaDto;
import fi.hel.verkkokauppa.message.dto.PaymentDto;
import org.apache.xmpbox.type.BadFieldValueException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class OrderConfirmationPDFTest {
    @Autowired
    OrderConfirmationPDF orderConfirmationPDF;
    @Test
    public void returnsGeneratedOrderConfirmationPDF() throws BadFieldValueException, IOException, TransformerException {
        GenerateOrderConfirmationPDFRequestDto dto = new GenerateOrderConfirmationPDFRequestDto();
        dto.setOrderId("oid1");
        dto.setCustomerFirstName("Teppo");
        dto.setCustomerLastName("Testaaja");
        dto.setCustomerEmail("teppo@testa.com");
        List<OrderItemDto> items = new ArrayList<>();
        dto.setItems(items);

        OrderItemDto item = new OrderItemDto();
        item.setProductLabel("Purukumi");
        item.setProductName("Hubba Bubba");
        item.setOriginalPriceGross("100");
        item.setPriceGross("50");
        item.setProductDescription("Mansikan makuinen purukumi");
        item.setQuantity(2);
        item.setVatPercentage("25,5");
        item.setRowPriceTotal("100.00");

        OrderItemDto item2 = new OrderItemDto();
        item2.setProductLabel("Product Label");
        item2.setProductName("Product Name");
        item2.setPriceGross("50");
        item2.setProductDescription("Product Description");
        item2.setQuantity(1);
        item2.setVatPercentage("25,5");
        item2.setRowPriceTotal("50,00");

        OrderItemMetaDto meta1 = new OrderItemMetaDto();
        List<OrderItemMetaDto> metaList1 = new ArrayList<>() ;
        meta1.setKey("meta avain1 not visible");
        meta1.setLabel("meta Label1 not visible");
        meta1.setValue("meta value1 not visible");
        meta1.setVisibleInCheckout("false");
        metaList1.add(meta1);
        item.setMeta(metaList1);

        List<OrderItemMetaDto> metaList2 = new ArrayList<>() ;
        OrderItemMetaDto meta2_1 = new OrderItemMetaDto();
        meta2_1.setKey("meta avain1");
        meta2_1.setLabel("meta Label1");
        meta2_1.setValue("meta value1");
        meta2_1.setVisibleInCheckout("true");
        metaList2.add(meta2_1);
        OrderItemMetaDto meta2_2 = new OrderItemMetaDto();
        meta2_2.setKey("meta avain2");
        meta2_2.setValue("meta value2");
        meta2_2.setVisibleInCheckout("true");
        metaList2.add(meta2_1);
        OrderItemMetaDto meta2_3 = new OrderItemMetaDto();
        meta2_3.setKey("meta avain2");
        meta2_3.setLabel("meta Label2");
        meta2_3.setVisibleInCheckout("true");
        metaList2.add(meta2_3);
        item2.setMeta(metaList2);

        items.add(item);
        items.add(item2);

        PaymentDto payment = new PaymentDto();
        payment.setCreatedAt(LocalDateTime.of(2021, 3, 23, 0, 0).toString());
        payment.setPaymentMethodLabel("Visa");
        payment.setTotal(new BigDecimal(150));
        payment.setTaxAmount(new BigDecimal("32.2"));
        payment.setTotalExclTax(payment.getTotal().subtract(payment.getTaxAmount()));

        dto.setPayment(payment);

        dto.setMerchantName("Testikauppias");
        dto.setMerchantStreetAddress("Testikatu 4 A 5");
        dto.setMerchantZipCode("01010");
        dto.setMerchantCity("Helsinki");
        dto.setMerchantEmail("testikauppias@testi.fi");
        dto.setMerchantBusinessId("1234567-1");
        dto.setMerchantPhoneNumber("010 0010010010");

        orderConfirmationPDF.generate("order-confirmation.pdf", dto);
    }
}
