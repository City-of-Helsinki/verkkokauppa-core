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

    private PDFA2A pdf = null;


    public void generate(String outputFile, GenerateOrderConfirmationPDFRequestDto dto) throws IOException, TransformerException, BadFieldValueException {
        pdf = new PDFA2A(TITLE);

        PDType0Font font = pdf.loadFont(PDType1Font.HELVETICA);
        PDType0Font boldFont = pdf.loadFont(PDType1Font.HELVETICA_BOLD);
        pdf.setColorProfile(OrderConfirmationPDF.class.getResourceAsStream("/sRGB2014.icc"), "sRGB IEC61966-2.1", "http://www.color.org");

        PDStructureElement currentElement = pdf.addDocumentStructureElement();
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.PART);
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.SECT);

        PDPage currentPage = pdf.addPage(pdf.createFontResources(font, "Helv"));

        float y = pdf.getUpperRightY(currentPage) - pdf.getStringHeight(font, 20) - 25;

        PDPageContentStream contentStream = pdf.createContentStream(currentPage);

        COSDictionary mc;

        //
        // RECEIPT HEADERS
        //
        addContentElement(contentStream, currentElement, currentPage, COSName.H, StandardStructureTypes.H,
                boldFont,
                20,
                SIDE_MARGIN,
                y,
                TITLE);


        addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                String.format("Kiitos tilauksestasi %s ", dto.getOrderId()));

        String[] dateAndTime = dto.getPayment().getCreatedAt().toString().split("T");
        addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                String.format("%s Tilausaika %s", dateAndTime[0], dateAndTime[1]));


//        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.TABLE);
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.TH);
        addContentElement(contentStream, currentElement, currentPage, COSName.H, StandardStructureTypes.H2,
                boldFont,
                16,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(boldFont, 16) + LINE_SPACING + 25),
                "Maksutiedot");

        for (OrderItemDto item : dto.getItems()) {
            // RECEIPT ITEMS

            if (item.getProductLabel() != null) {
                currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
                addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                        font,
                        16,
                        SIDE_MARGIN,
                        y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                        item.getProductLabel());
            }

            currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
            addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                    boldFont,
                    FONT_SIZE,
                    SIDE_MARGIN,
                    y -= (pdf.getStringHeight(boldFont, FONT_SIZE) + LINE_SPACING),
                    item.getProductName());

            currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);

            if (item.getOriginalPriceGross() != null) {
                String originalPriceGross = String.format("%s €", item.getOriginalPriceGross());
                addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                        font,
                        FONT_SIZE,
                        pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, originalPriceGross) - 1,
                        y,
                        originalPriceGross,
                        "Alkuperäinen bruttohinta");

                mc = pdf.beginMarkedContent(contentStream, COSName.ARTIFACT);
                contentStream.moveTo(pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, originalPriceGross) - 2, y + pdf.getStringHeight(font, FONT_SIZE) / 2);
                contentStream.lineTo(pdf.getUpperRightX(currentPage) - SIDE_MARGIN, y + pdf.getStringHeight(font, FONT_SIZE) / 2);
                contentStream.closeAndStroke();
                pdf.endMarkedContent(contentStream);
                pdf.addContentStructureElement(currentElement, COSName.ARTIFACT, null, mc, currentPage);

                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING);
            }

            String priceGross = String.format("%s € / %s", item.getPriceGross(), "kpl");
            addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                    font,
                    FONT_SIZE,
                    pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, priceGross) - 1,
                    y,
                    priceGross);

            if (item.getProductDescription() != null) {
                currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
                addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                        font,
                        FONT_SIZE,
                        SIDE_MARGIN,
                        y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                        item.getProductDescription());
            }

            currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
            addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                    font,
                    FONT_SIZE,
                    SIDE_MARGIN,
                    y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                    String.format("%d %s yhteensä Sis. alv (%s%%)", item.getQuantity(), "kpl", item.getVatPercentage()));

            String rowPriceTotal = String.format("%s €", item.getRowPriceTotal());
            currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
            addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                    font,
                    FONT_SIZE,
                    pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, rowPriceTotal),
                    y,
                    rowPriceTotal);

            y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING);
        }

        //
        // TOTAL PRICE AND PAYMENT INFO
        //
        currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
        addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                "Maksettu yhteensä");

        String priceTotal = String.format("%s €", dto.getPayment().getTotal());
        currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
        addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, priceTotal),
                y,
                priceTotal);

        currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
        addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                String.format("Sis. alv (%s%%)", dto.getItems().get(0).getVatPercentage()));

        String alvTotal = String.format("%s €", dto.getPayment().getTaxAmount());
        currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
        addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, alvTotal),
                y,
                alvTotal);

        currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
        addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                "Maksutapa");

        currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
        addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, dto.getPayment().getPaymentMethodLabel()),
                y,
                dto.getPayment().getPaymentMethodLabel());

        currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
        addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= ((pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING)*3/2 ),
                "Päivämäärä");

        String[] paymentDateAndTime = dto.getPayment().getCreatedAt().toString().split("T");
        currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
        addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, paymentDateAndTime[0]),
                y += ((pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING)/2),
                paymentDateAndTime[0]);
        currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
        addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, paymentDateAndTime[1]),
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                paymentDateAndTime[1]);

        currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.TD);
        addContentElement(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                "Maksutapa"); */

        currentElement = pdf.addStructureElement((PDStructureElement) currentElement.getParent(), StandardStructureTypes.P);
        addDivider(contentStream, currentElement, currentPage, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING));

        // CUSTOMER INFO


        // MERCHANT INFO

        pdf.closeContentStream(contentStream);

        pdf.save(outputFile);
    }

    private void addContentElement(PDPageContentStream contentStream, PDStructureElement currentElement, PDPage currentPage,
                                   COSName markedContentCosName, String standardStructureType,
                                   PDType0Font font, float fontSize, float tx, float ty, String text) throws IOException {
        addContentElement (contentStream, currentElement, currentPage,
                markedContentCosName, standardStructureType,
                font, fontSize, tx, ty, text, null);
    }

    private void addContentElement(PDPageContentStream contentStream, PDStructureElement currentElement, PDPage currentPage,
                                   COSName markedContentCosName, String standardStructureType,
                                   PDType0Font font, float fontSize, float tx, float ty, String text, String alternateDescription) throws IOException {
        COSDictionary mc = pdf.beginMarkedContent(contentStream, markedContentCosName);
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(tx, ty);
        contentStream.showText(text);
        contentStream.endText();
        pdf.endMarkedContent(contentStream);
        if( alternateDescription != null ) {
            pdf.addContentStructureElement(currentElement, markedContentCosName, standardStructureType, mc, currentPage).setAlternateDescription(alternateDescription);
        } else{
            pdf.addContentStructureElement(currentElement, markedContentCosName, standardStructureType, mc, currentPage);
        }

    }

    private void addDivider(PDPageContentStream contentStream, PDStructureElement currentElement, PDPage currentPage,
                                   COSName markedContentCosName, String standardStructureType,
                                   PDType0Font font, float fontSize, float tx, float ty) throws IOException {
        COSDictionary mc = pdf.beginMarkedContent(contentStream, markedContentCosName);
        contentStream.setFont(font, fontSize);
        contentStream.setLineWidth(1);
        contentStream.moveTo(SIDE_MARGIN, ty);
        contentStream.lineTo(pdf.getUpperRightX(currentPage) - SIDE_MARGIN, ty);
        contentStream.stroke();
        pdf.endMarkedContent(contentStream);
        pdf.addContentStructureElement(currentElement, markedContentCosName, standardStructureType, mc, currentPage);

    }
}
