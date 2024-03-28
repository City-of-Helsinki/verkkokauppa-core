package fi.hel.verkkokauppa.message.service;
import fi.hel.verkkokauppa.message.dto.GenerateOrderConfirmationPDFRequestDto;
import fi.hel.verkkokauppa.message.dto.OrderItemDto;
import fi.hel.verkkokauppa.message.model.PDFA2A;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.StandardStructureTypes;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.xmpbox.type.BadFieldValueException;
import org.springframework.stereotype.Component;

import javax.xml.transform.TransformerException;
import java.io.IOException;

@Component
public class OrderConfirmationPDF {

    private final int SIDE_MARGIN = 120;
    private final String TITLE = "Tilausvahvistus ja kuitti";

    private final int FONT_SIZE = 12;

    private final int LINE_SPACING = 10;


    public void generate(String outputFile, GenerateOrderConfirmationPDFRequestDto dto) throws IOException, TransformerException, BadFieldValueException {
        PDFA2A pdf = new PDFA2A(TITLE);

        PDType0Font font = pdf.loadFont(PDType1Font.HELVETICA);
        pdf.setColorProfile(OrderConfirmationPDF.class.getResourceAsStream("/sRGB2014.icc"), "sRGB IEC61966-2.1", "http://www.color.org");

        PDStructureElement currentElement = pdf.addDocumentStructureElement();
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.PART);
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.SECT);

        PDPage currentPage = pdf.addPage(pdf.createFontResources(font, "Helv"));

        float y = pdf.getUpperRightY(currentPage) - pdf.getStringHeight(font, 20) - 25;

        PDPageContentStream contentStream = pdf.createContentStream(currentPage);

        COSDictionary mc = pdf.beginMarkedContent(contentStream, COSName.H);
        contentStream.beginText();
        contentStream.setFont(font, 20);
        contentStream.newLineAtOffset(SIDE_MARGIN, y);
        contentStream.showText(TITLE);
        contentStream.endText();
        pdf.endMarkedContent(contentStream);
        pdf.addContentStructureElement(currentElement, COSName.H, StandardStructureTypes.H, mc, currentPage);


        mc = pdf.beginMarkedContent(contentStream, COSName.P);
        contentStream.beginText();
        contentStream.setFont(font, FONT_SIZE);
        contentStream.newLineAtOffset(SIDE_MARGIN, y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING));
        contentStream.showText(String.format("Kiitos tilauksestasi %s %s Tilausaika %s", dto.getOrderId(), dto.getCreatedAt(), dto.getCreatedAt()));
        contentStream.endText();
        pdf.endMarkedContent(contentStream);
        pdf.addContentStructureElement(currentElement, COSName.P, StandardStructureTypes.P, mc, currentPage);

        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.TABLE);
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.TH);
        mc = pdf.beginMarkedContent(contentStream, COSName.H);
        contentStream.beginText();
        contentStream.setFont(font, 16);
        contentStream.newLineAtOffset(SIDE_MARGIN, y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING + 25));
        contentStream.showText("Maksutiedot");
        contentStream.endText();
        pdf.endMarkedContent(contentStream);
        pdf.addContentStructureElement(currentElement, COSName.H, StandardStructureTypes.H2, mc, currentPage);

        for (OrderItemDto item : dto.getItems()) {
            if (item.getProductLabel() != null) {
                currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
                mc = pdf.beginMarkedContent(contentStream, COSName.P);
                contentStream.beginText();
                contentStream.setFont(font, FONT_SIZE);
                contentStream.newLineAtOffset(SIDE_MARGIN, y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING));
                contentStream.showText(item.getProductLabel());
                contentStream.endText();
                pdf.endMarkedContent(contentStream);
                pdf.addContentStructureElement(currentElement, COSName.P, StandardStructureTypes.P, mc, currentPage);
            }

            currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
            mc = pdf.beginMarkedContent(contentStream, COSName.P);
            contentStream.beginText();
            contentStream.setFont(font, FONT_SIZE);
            contentStream.newLineAtOffset(SIDE_MARGIN, y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING));
            contentStream.showText(item.getProductName());
            contentStream.endText();
            pdf.endMarkedContent(contentStream);
            pdf.addContentStructureElement(currentElement, COSName.P, StandardStructureTypes.P, mc, currentPage);

            currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
            if (item.getOriginalPriceGross() != null) {
                String originalPriceGross = String.format("%s €", item.getOriginalPriceGross());
                mc = pdf.beginMarkedContent(contentStream, COSName.P);
                contentStream.beginText();
                contentStream.setFont(font, FONT_SIZE);
                contentStream.newLineAtOffset(pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, originalPriceGross) - 1, y);
                contentStream.showText(originalPriceGross);
                contentStream.endText();
                pdf.endMarkedContent(contentStream);
                pdf.addContentStructureElement(currentElement, COSName.P, StandardStructureTypes.P, mc, currentPage).setAlternateDescription("Alkuperäinen bruttohinta");

                mc = pdf.beginMarkedContent(contentStream, COSName.ARTIFACT);
                contentStream.moveTo(pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, originalPriceGross) - 2, y + pdf.getStringHeight(font, FONT_SIZE) / 2);
                contentStream.lineTo(pdf.getUpperRightX(currentPage) - SIDE_MARGIN, y + pdf.getStringHeight(font, FONT_SIZE) / 2);
                contentStream.closeAndStroke();
                pdf.endMarkedContent(contentStream);
                pdf.addContentStructureElement(currentElement, COSName.ARTIFACT, null, mc, currentPage);

                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING);
            }

            String priceGross = String.format("%s € / %s", item.getPriceGross(), "kpl");
            mc = pdf.beginMarkedContent(contentStream, COSName.P);
            contentStream.beginText();
            contentStream.setFont(font, FONT_SIZE);
            contentStream.newLineAtOffset(pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, priceGross) - 1, y);
            contentStream.showText(priceGross);
            contentStream.endText();
            pdf.endMarkedContent(contentStream);
            pdf.addContentStructureElement(currentElement, COSName.P, StandardStructureTypes.P, mc, currentPage);

            if (item.getProductDescription() != null) {
                currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
                mc = pdf.beginMarkedContent(contentStream, COSName.P);
                contentStream.beginText();
                contentStream.setFont(font, FONT_SIZE);
                contentStream.newLineAtOffset(SIDE_MARGIN, y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING));
                contentStream.showText(item.getProductDescription());
                contentStream.endText();
                pdf.endMarkedContent(contentStream);
                pdf.addContentStructureElement(currentElement, COSName.P, StandardStructureTypes.P, mc, currentPage);
            }

            currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
            mc = pdf.beginMarkedContent(contentStream, COSName.P);
            contentStream.beginText();
            contentStream.setFont(font, FONT_SIZE);
            contentStream.newLineAtOffset(SIDE_MARGIN, y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING));
            contentStream.showText(String.format("%d %s yhteensä Sis. alv (%s%%)", item.getQuantity(), "kpl", item.getVatPercentage()));
            contentStream.endText();
            pdf.endMarkedContent(contentStream);
            pdf.addContentStructureElement(currentElement, COSName.P, StandardStructureTypes.P, mc, currentPage);

            String rowPriceTotal = String.format("%s €", item.getRowPriceTotal());
            currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
            mc = pdf.beginMarkedContent(contentStream, COSName.P);
            contentStream.beginText();
            contentStream.setFont(font, FONT_SIZE);
            contentStream.newLineAtOffset(pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, rowPriceTotal), y);
            contentStream.showText(rowPriceTotal);
            contentStream.endText();
            pdf.endMarkedContent(contentStream);
            pdf.addContentStructureElement(currentElement, COSName.P, StandardStructureTypes.P, mc, currentPage);

        }


        pdf.closeContentStream(contentStream);

        pdf.save(outputFile);
    }
}
