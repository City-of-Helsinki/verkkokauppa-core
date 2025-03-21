package fi.hel.verkkokauppa.message.service;

import fi.hel.verkkokauppa.message.dto.GenerateOrderConfirmationPDFRequestDto;
import fi.hel.verkkokauppa.message.dto.OrderItemDto;
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
        List<OrderItemDto> items = new ArrayList<>();
        dto.setItems(items);

        OrderItemDto item = new OrderItemDto();
        item.setProductLabel("Purukumi");
        item.setProductName("Hubba Bubba");
        item.setOriginalPriceGross("100");
        item.setPriceGross("50");
        item.setProductDescription("Product Description");
        item.setQuantity(2);
        item.setVatPercentage("25,5");
        item.setRowPriceTotal("100.00");
        items.add(item);

        OrderItemDto item2 = new OrderItemDto();
        item2.setProductLabel("Product Label");
        item2.setProductName("Product Name");
        item2.setPriceGross("50");
        item2.setProductDescription("Product Description");
        item2.setQuantity(1);
        item2.setVatPercentage("25,5");
        item2.setRowPriceTotal("50.00");
        items.add(item2);

        PaymentDto payment = new PaymentDto();
        payment.setCreatedAt(LocalDateTime.of(2021, 3, 23, 0, 0));
        payment.setPaymentMethodLabel("Visa");
        payment.setTotal(new BigDecimal(150));
        payment.setTaxAmount(new BigDecimal("32.2"));
        payment.setTotalExclTax(payment.getTotal().subtract(payment.getTaxAmount()));

        dto.setPayment(payment);

        orderConfirmationPDF.generate("order-confirmation.pdf", dto);
    }
}
