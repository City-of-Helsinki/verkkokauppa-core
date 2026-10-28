package fi.hel.verkkokauppa.order.service.order;


import fi.hel.verkkokauppa.common.rest.dto.payment.PaymentDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemMetaDto;
import fi.hel.verkkokauppa.order.model.pdf.GenerateOrderConfirmationPDFRequestDto;
import fi.hel.verkkokauppa.order.service.pdf.OrderConfirmationPDF;
import org.apache.xmpbox.type.BadFieldValueException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class OrderConfirmationPDFTest {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmationPDFTest.class);
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

    @Test
    public void testGenerateMultipleOrderConfirmationPdfsWithLineWrapping() throws Exception {

        List<GenerateOrderConfirmationPDFRequestDto> dtos = List.of(
                createPdfOrderDto("AAA"),
                createPdfOrderDto("BBB"),
                createPdfOrderDto("CCC"),
                createPdfOrderDto("DDD"),
                createPdfOrderDto("EEE"),
                createPdfOrderDto("FFF"),
                createPdfOrderDto("GGG"),
                createPdfOrderDto("HHH"),
                createPdfOrderDto("III"),
                createPdfOrderDto("JJJ")
        );

        ExecutorService executor = Executors.newFixedThreadPool(dtos.size());

        CountDownLatch readyLatch = new CountDownLatch(dtos.size());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(dtos.size());

        List<Future<Void>> futures = new ArrayList<>();

        for (GenerateOrderConfirmationPDFRequestDto dto : dtos) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                try {
                    String pdfName = dto.getOrderId() + "-order-confirmation-thread.pdf";
                    byte[] pdfArray = orderConfirmationPDF.generate(pdfName, dto);
                    if (pdfArray != null) {
                        log.info("Pdf generated.");
                    } else {
                        log.warn("Generated pdf array is null.");
                    }
                } finally {
                    doneLatch.countDown();
                }
                return null;
            }));
        }

        // Wait for the threads to be ready
        readyLatch.await();

        // Release all threads at the same time
        startLatch.countDown();

        // Wait for the threads to finish
        doneLatch.await();

        // Make sure all exceptions are noticed
        for (Future<Void> future : futures) {
            future.get();
        }
        executor.shutdown();

    }

    private GenerateOrderConfirmationPDFRequestDto createPdfOrderDto(String orderId) {

        GenerateOrderConfirmationPDFRequestDto dto = new GenerateOrderConfirmationPDFRequestDto();
        dto.setOrderId(orderId);
        dto.setCustomerFirstName(orderId + "FirstName");
        dto.setCustomerLastName(orderId + "LastName");
        dto.setCustomerEmail(orderId + "-pdf@testitesti.com");
        List<OrderItemDto> items = new ArrayList<>();
        dto.setItems(items);

        OrderItemDto item = new OrderItemDto();
        item.setProductLabel(orderId + "Product Label 1 Vaka vanha Väinämöinen käski nuoren, käski vanhan, käski keskikertaisenki soittamahan sormillansa, tuota ruotaista romua, kalanluista kanteletta.");
        item.setProductName(orderId + "Product Name 1 Sanoi vanha Väinämöinen: Ei ole tässä nuorisossa, kansassa kasuavassa eikä vanhassa väessä tuon on soiton soittajaista, tuon ilon iloajaista. Joko Pohjola paremmin saisi soiton soittamahan, tuon ilon iloamahan, jospa laitan Pohjolahan?");
        item.setOriginalPriceGross(orderId);
        item.setPriceGross(orderId);
        item.setProductDescription(orderId + "Product Description 1");
        item.setQuantity(2);
        item.setVatPercentage("25,5");
        item.setRowPriceTotal("100");

        OrderItemDto item2 = new OrderItemDto();
        item2.setProductLabel(orderId + "Product Label 2 Siitä vanha Väinämöinen alkoi soittoa somasti, hauinruotaista romua, kalanluista kanteletta. Sormet nousi notkeasti, peukalo ylös keveni.");
        item2.setProductName(orderId + "Product Name 2 Jo kävi ilo ilolle, riemu riemulle remahti, tuntui soitto soitannalle, laulu laululle tehosi. Helähteli hauin hammas, kalan pursto purkaeli, ulvosi upehen jouhet, jouhet ratsun raikkahuivat.");
        item2.setPriceGross(orderId);
        item2.setProductDescription(orderId + "Product Description 2");
        item2.setQuantity(1);
        item2.setVatPercentage("25,5");
        item2.setRowPriceTotal("100");

        OrderItemMetaDto meta1 = new OrderItemMetaDto();
        List<OrderItemMetaDto> metaList1 = new ArrayList<>() ;
        meta1.setKey(orderId + " meta avain1 not visible");
        meta1.setLabel(orderId + " meta Label1 not visible");
        meta1.setValue(orderId + " meta value1 not visible");
        meta1.setVisibleInCheckout("false");
        metaList1.add(meta1);
        item.setMeta(metaList1);

        List<OrderItemMetaDto> metaList2 = new ArrayList<>() ;
        OrderItemMetaDto meta2_1 = new OrderItemMetaDto();
        meta2_1.setKey(orderId + " meta avain1");
        meta2_1.setLabel(orderId + " meta Label1 Se on seppo Ilmarinen päivän kolmannen perästä kallistihe katsomahan ahjonsa alaista puolta: näki sammon syntyväksi, kirjokannen kasvavaksi.");
        meta2_1.setValue(orderId + " meta value1 Siitä seppo Ilmarinen, takoja iän-ikuinen, takoa taputtelevi, lyöä lynnähyttelevi. Takoi sammon taitavasti: laitahan on jauhomyllyn, toisehen on suolamyllyn, rahamyllyn kolmantehen.");
        meta2_1.setVisibleInCheckout("true");
        metaList2.add(meta2_1);
        OrderItemMetaDto meta2_2 = new OrderItemMetaDto();
        meta2_2.setKey(orderId + " meta avain2");
        meta2_2.setValue(orderId + " meta value2");
        meta2_2.setVisibleInCheckout("true");
        metaList2.add(meta2_2);
        OrderItemMetaDto meta2_3 = new OrderItemMetaDto();
        meta2_3.setKey(orderId + " meta avain2");
        meta2_3.setLabel(orderId + " meta Label2");
        meta2_3.setVisibleInCheckout("true");
        metaList2.add(meta2_3);
        item2.setMeta(metaList2);

        items.add(item);
        items.add(item2);

        PaymentDto payment = new PaymentDto();
        payment.setCreatedAt(LocalDateTime.of(2021, 3, 23, 0, 0).toString());
        payment.setPaymentMethodLabel(orderId + "-Visa");
        payment.setTotal(new BigDecimal(150));
        payment.setTaxAmount(new BigDecimal("32.2"));
        payment.setTotalExclTax(payment.getTotal().subtract(payment.getTaxAmount()));

        dto.setPayment(payment);

        dto.setMerchantName("Testikauppias-" + orderId + " Louhi, Pohjolan emäntä, Pohjan akka harvahammas");
        dto.setMerchantStreetAddress("Testikatu 4 A 5");
        dto.setMerchantZipCode("01010");
        dto.setMerchantCity("Helsinki");
        dto.setMerchantEmail("testikauppias@testitesti.fi");
        dto.setMerchantBusinessId("1234567-1-" + orderId);
        dto.setMerchantPhoneNumber("010 0010010010");

        return dto;
    }
}
