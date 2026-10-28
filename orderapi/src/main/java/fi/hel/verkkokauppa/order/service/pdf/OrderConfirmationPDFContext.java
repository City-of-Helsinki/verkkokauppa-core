package fi.hel.verkkokauppa.order.service.pdf;

import fi.hel.verkkokauppa.order.model.pdf.PDFA2A;
import lombok.Getter;
import lombok.Setter;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

@Getter
public class OrderConfirmationPDFContext {

    private final PDFA2A pdf;
    @Setter
    private PDPage currentPage;
    @Setter
    private PDPageContentStream contentStream;

    public OrderConfirmationPDFContext(PDFA2A pdf) {
        this.pdf = pdf;
    }


}
