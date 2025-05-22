package fi.hel.verkkokauppa.order.api.pdf;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.order.model.pdf.GenerateOrderConfirmationPDFRequestDto;
import fi.hel.verkkokauppa.order.service.pdf.OrderConfirmationPDF;
import fi.hel.verkkokauppa.order.service.pdf.PdfGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@Validated
public class PdfController {
    private final Logger log = LoggerFactory.getLogger(PdfController.class);

    @Autowired
    private PdfGenerationService service;

    @Autowired
    private SendNotificationService sendNotificationService;

    @Autowired
    private OrderConfirmationPDF orderConfirmationPdf;

    @GetMapping(value = "order/pdf/orderConfirmation", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateOrderConfirmationPdf(@RequestParam(value = "orderId") String orderId) {
        byte[] pdfArray = null;
        try {
            GenerateOrderConfirmationPDFRequestDto dto = service.getPDFRequestDto(orderId);

            pdfArray = orderConfirmationPdf.generate("order-confirmation.pdf", dto);
        } catch (Exception e){
            log.error("Error occurred while generating PDF receipt", e);
            sendNotificationService.sendErrorNotification("Error occurred while generating PDF receipt", e.toString());
            Error error = new Error("failed-to-create-pdf-receipt", "failed to create pdf receipt");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "generateOrderConfirmation.pdf");

        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(pdfArray);
    }

}
