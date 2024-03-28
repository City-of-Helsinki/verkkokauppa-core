package fi.hel.verkkokauppa.message.service;

import fi.hel.verkkokauppa.message.dto.GenerateOrderConfirmationPDFRequestDto;
import fi.hel.verkkokauppa.message.dto.OrderItemDto;
import org.apache.xmpbox.type.BadFieldValueException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.xml.transform.TransformerException;
import java.io.IOException;
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
        dto.setCreatedAt("31-01-2022T12:22:22");
        List<OrderItemDto> items = new ArrayList<>();
        dto.setItems(items);
        OrderItemDto item = new OrderItemDto();
        items.add(item);
        item.setProductLabel("Product Label");
        item.setProductName("Product Name");
        item.setOriginalPriceGross("100");
        item.setPriceGross("50");
        item.setProductDescription("Product Description");
        item.setQuantity(1);
        item.setVatPercentage("24");
        item.setRowPriceTotal("500");
        orderConfirmationPDF.generate("order-confirmation.pdf", dto);
    }
}
