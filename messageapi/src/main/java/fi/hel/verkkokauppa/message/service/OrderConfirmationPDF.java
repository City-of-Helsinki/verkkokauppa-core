package fi.hel.verkkokauppa.message.service;
import fi.hel.verkkokauppa.message.dto.GenerateOrderConfirmationPDFRequestDto;
import fi.hel.verkkokauppa.message.dto.OrderItemDto;
import fi.hel.verkkokauppa.message.dto.OrderItemMetaDto;
import fi.hel.verkkokauppa.message.model.PDFA2A;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.StandardStructureTypes;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationText;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.xmpbox.type.BadFieldValueException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.transform.TransformerException;
import java.awt.*;
import java.io.IOException;

@Component
public class OrderConfirmationPDF {

    private final int SIDE_MARGIN = 120;
    private final int BOTTOM_MARGIN = 50;
    private final int UPPER_MARGIN = 50;
    private final String TITLE = "Tilausvahvistus ja kuitti";

    private final int FONT_SIZE = 12;

    private final int LINE_SPACING = 10;

    private PDFA2A pdf = null;
    private PDPage currentPage = null;
    private PDPageContentStream contentStream = null;

    @Value("${test.pdf.save:false}")
    private boolean saveTestPDF;

    public byte[] generate(String outputFile, GenerateOrderConfirmationPDFRequestDto dto) throws IOException, TransformerException, BadFieldValueException {
        pdf = new PDFA2A(TITLE);

        PDType0Font font = pdf.loadFont(PDType1Font.HELVETICA);
        PDType0Font boldFont = pdf.loadFont(PDType1Font.HELVETICA_BOLD);
        pdf.setColorProfile(OrderConfirmationPDF.class.getResourceAsStream("/sRGB2014.icc"), "sRGB IEC61966-2.1", "http://www.color.org");

        PDStructureElement currentElement = pdf.addDocumentStructureElement();
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.PART);
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.SECT);

        currentPage = pdf.addPage(pdf.createFontResources(font, "Helv"));

        float y = pdf.getUpperRightY(currentPage) - pdf.getStringHeight(font, 20) - 25;

        contentStream = pdf.createContentStream(currentPage);

        COSDictionary mc;

        //
        // RECEIPT HEADERS
        //
        y = addContentElement(currentElement, COSName.H, StandardStructureTypes.H,
                boldFont,
                20,
                SIDE_MARGIN,
                y,
                TITLE);


        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                String.format("Kiitos tilauksestasi %s ", dto.getOrderId()));

        String[] dateAndTime = dto.getPayment().getCreatedAt().toString().split("T");
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                String.format("%s Tilausaika %s", dateAndTime[0], dateAndTime[1]));


//        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.TABLE);
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.H2);
        y = addContentElement(currentElement, COSName.H, StandardStructureTypes.H2,
                boldFont,
                16,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(boldFont, 16) + LINE_SPACING + 25),
                "Maksutiedot");

        for (OrderItemDto item : dto.getItems()) {

            // RECEIPT ITEMS

            if (item.getProductLabel() != null) {
                currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
                y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                        font,
                        16,
                        SIDE_MARGIN,
                        y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                        item.getProductLabel());
            }

            currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
            y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                    boldFont,
                    FONT_SIZE,
                    SIDE_MARGIN,
                    y -= (pdf.getStringHeight(boldFont, FONT_SIZE) + LINE_SPACING),
                    item.getProductName());

            currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);

            if (item.getOriginalPriceGross() != null) {
                String originalPriceGross = String.format("%s €", formatNumber(item.getOriginalPriceGross(), 2));
                y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
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
                pdf.addContentStructureElement(currentElement, COSName.ARTIFACT, StandardStructureTypes.P, mc, currentPage);

                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING);
            }

            String priceGross = String.format("%s € / %s", formatNumber(item.getPriceGross(),2), "kpl");
            y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                    font,
                    FONT_SIZE,
                    pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, priceGross) - 1,
                    y,
                    priceGross);

            if (item.getProductDescription() != null) {
                currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.TD);
                y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                        font,
                        FONT_SIZE,
                        SIDE_MARGIN,
                        y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                        item.getProductDescription());
            }

            currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.TD);
            y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                    font,
                    FONT_SIZE,
                    SIDE_MARGIN,
                    y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                    String.format("%d %s yhteensä Sis. alv (%s%%)", item.getQuantity(), "kpl", formatNumber(item.getVatPercentage(),2)));

            String rowPriceTotal = String.format("%s €", formatNumber(item.getRowPriceTotal(),2));
            currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.TD);
            y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                    font,
                    FONT_SIZE,
                    pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, rowPriceTotal),
                    y,
                    rowPriceTotal);

            // Metadata
            for (OrderItemMetaDto meta : item.getMeta()) {
                if( meta.getVisibleInCheckout() != null && meta.getVisibleInCheckout().equalsIgnoreCase("true") ){
                    if( meta.getLabel() != null ) {
                        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
                        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                                font,
                                FONT_SIZE,
                                SIDE_MARGIN,
                                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                                meta.getLabel());
                    }
                    if( meta.getValue() != null ) {
                        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
                        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                                font,
                                FONT_SIZE,
                                SIDE_MARGIN,
                                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                                meta.getValue());
                    }
                }
            }

            y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING);

        }

        //
        // TOTAL PRICE AND PAYMENT INFO
        //
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                "Maksettu yhteensä");

        String priceTotal = String.format("%s €", formatNumber(dto.getPayment().getTotal().toString(),2));
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, priceTotal),
                y,
                priceTotal);

        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                String.format("Sis. alv (%s%%)", formatNumber(dto.getItems().get(0).getVatPercentage(),2)));

        String alvTotal = String.format("%s €", formatNumber(dto.getPayment().getTaxAmount().toString(), 2));
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, alvTotal),
                y,
                alvTotal);

        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                "Maksutapa");

        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, dto.getPayment().getPaymentMethodLabel()),
                y,
                dto.getPayment().getPaymentMethodLabel());

        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= ((pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING)*3/2 ),
                "Päivämäärä");

        String[] paymentDateAndTime = dto.getPayment().getCreatedAt().toString().split("T");
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, paymentDateAndTime[0]),
                y += ((pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING)/2),
                paymentDateAndTime[0]);
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                pdf.getUpperRightX(currentPage) - SIDE_MARGIN - pdf.getStringWidth(font, FONT_SIZE, paymentDateAndTime[1]),
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                paymentDateAndTime[1]);

        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addDivider(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING));

        // CUSTOMER INFO
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.H2);
        y = addContentElement(currentElement, COSName.H, StandardStructureTypes.H2,
                boldFont,
                16,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(boldFont, 16) + LINE_SPACING),
                "Tilaajan tiedot");

        String customerName = String.format("%s %s", dto.getCustomerFirstName(), dto.getCustomerLastName());
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                customerName);

        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                dto.getCustomerEmail(),
                Color.blue);

        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addDivider(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING));

        // MERCHANT INFO
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.H2);
        y = addContentElement(currentElement, COSName.H, StandardStructureTypes.H2,
                boldFont,
                16,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(boldFont, 16) + LINE_SPACING),
                "Myyjän tiedot");

        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                dto.getMerchantName());

        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                dto.getMerchantStreetAddress());

        String merchantAddress = String.format("%s %s", dto.getMerchantZipCode(), dto.getMerchantCity());
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                merchantAddress);

        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                dto.getMerchantEmail(),
                Color.blue);

        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                dto.getMerchantPhoneNumber());

        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                dto.getMerchantBusinessId());

        // FOOTER
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING + LINE_SPACING),
                "Mikäli sinulla on tilaukseen liittyen kysymyksiä kohdistathan ne suoraan");

        String footerMerchant = String.format("myyjälle %s", dto.getMerchantName());
        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                footerMerchant);

        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING + LINE_SPACING),
                "Myyjä toimittaa tarvittaessa erikseen lisäohjeet palvelun/tuotteen käyttöön");

        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement, COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdf.getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                "tai toimitukseen liittyen.");



        // CLOSE PDF
        pdf.closeContentStream(contentStream);

        byte[] pdfArray = pdf.getPDFByteArray();
        pdf.save(outputFile, saveTestPDF);

        return pdfArray;
    }

    private float addContentElement(PDStructureElement currentElement,
                                   COSName markedContentCosName, String standardStructureType,
                                   PDType0Font font, float fontSize, float tx, float ty, String text) throws IOException {
        return addContentElement (currentElement,
                markedContentCosName, standardStructureType,
                font, fontSize, tx, ty, text, null, null, true);
    }

    private float addContentElement(PDStructureElement currentElement,
                                   COSName markedContentCosName, String standardStructureType,
                                   PDType0Font font, float fontSize, float tx, float ty, String text, Color colour) throws IOException {
        return addContentElement (currentElement,
                markedContentCosName, standardStructureType,
                font, fontSize, tx, ty, text, null, colour, true);
    }

    private float addContentElement(PDStructureElement currentElement,
                                   COSName markedContentCosName, String standardStructureType,
                                   PDType0Font font, float fontSize, float tx, float ty, String text, String alternateDescription) throws IOException {
        return addContentElement (currentElement,
                markedContentCosName, standardStructureType,
                font, fontSize, tx, ty, text, alternateDescription, null, true);
    }

    private float addContentElement(PDStructureElement currentElement,
                                    COSName markedContentCosName, String standardStructureType,
                                    PDType0Font font, float fontSize, float tx, float ty, String text, String alternateDescription, Color colour, Boolean pageCheck) throws IOException {
        if( pageCheck && ty <= BOTTOM_MARGIN ){
            currentPage = pdf.addPage(pdf.createFontResources(font, "Helv"));

            contentStream.close();
            contentStream = pdf.createContentStream(currentPage);

            ty = pdf.getUpperRightY(currentPage) - pdf.getStringHeight(font, 20) - 25;
        }
        if( text == null ){
            return ty;
        }
        COSDictionary mc = pdf.beginMarkedContent(contentStream, markedContentCosName);
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(tx, ty);

        if( colour != null ) {
            contentStream.setNonStrokingColor(colour);
        }
        else {
            contentStream.setNonStrokingColor(Color.black);
        }

        contentStream.showText(text);
        contentStream.endText();
        pdf.endMarkedContent(contentStream);
        if( alternateDescription != null ) {
            pdf.addContentStructureElement(currentElement, markedContentCosName, standardStructureType, mc, currentPage).setAlternateDescription(alternateDescription);
        } else{
            pdf.addContentStructureElement(currentElement, markedContentCosName, standardStructureType, mc, currentPage);
        }

        return ty;
    }

    private float addLink(PDStructureElement currentElement,
                          COSName markedContentCosName, String standardStructureType,
                                   PDType0Font font, float fontSize, float tx, float ty, String text, String linkUrl) throws IOException {
        if( ty <= BOTTOM_MARGIN ){
            currentPage = pdf.addPage(pdf.createFontResources(font, "Helv"));

            contentStream.close();
            contentStream = pdf.createContentStream(currentPage);

            ty = pdf.getUpperRightY(currentPage) - pdf.getStringHeight(font, 20) - 25;
        }
        if( text == null ){
            return ty;
        }
        COSDictionary mc = pdf.beginMarkedContent(contentStream, markedContentCosName);
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.setNonStrokingColor(Color.blue);
        contentStream.newLineAtOffset(tx, ty);
        contentStream.showText(text);
        contentStream.endText();

        float textWidth = pdf.getStringWidth(font, FONT_SIZE, text);

        // Draw the underline
        float underlineY = ty - 2; // Slightly below text
        contentStream.setLineWidth(1);
        contentStream.moveTo(tx, underlineY);
        contentStream.lineTo(tx +  textWidth, underlineY);
        contentStream.stroke();

        // Create the link annotation
        PDAnnotationLink link = new PDAnnotationLink();
        link.setAnnotationFlags(4);
        PDRectangle linkRect = new PDRectangle(tx, ty - fontSize, textWidth, fontSize);
        link.setRectangle(linkRect);

        // Define the border style (no visible border)
        PDBorderStyleDictionary borderStyle = new PDBorderStyleDictionary();
        borderStyle.setWidth(0); // No visible border
        link.setBorderStyle(borderStyle);

        // create link action
        PDActionURI action = new PDActionURI();
        action.setURI(linkUrl);
        link.setAction(action);

        currentPage.getAnnotations().add(link);

        pdf.endMarkedContent(contentStream);
        pdf.addContentStructureElement(currentElement, markedContentCosName, standardStructureType, mc, currentPage);

        return ty;
    }

    private float addDivider(PDStructureElement currentElement,
                                   COSName markedContentCosName, String standardStructureType,
                                   PDType0Font font, float fontSize, float ty) throws IOException {
        if( ty <= BOTTOM_MARGIN ){
            currentPage = pdf.addPage(pdf.createFontResources(font, "Helv"));

            contentStream.close();
            contentStream = pdf.createContentStream(currentPage);
            contentStream.setFont(font, fontSize);

            ty = pdf.getUpperRightY(currentPage) - pdf.getStringHeight(font, 20) - 25;
        }
        COSDictionary mc = pdf.beginMarkedContent(contentStream, markedContentCosName);
        contentStream.setFont(font, fontSize);
        contentStream.setLineWidth(1);
        contentStream.moveTo(SIDE_MARGIN-15, ty);
        contentStream.lineTo(pdf.getUpperRightX(currentPage) - (SIDE_MARGIN-15), ty);
        contentStream.stroke();
        pdf.endMarkedContent(contentStream);
        pdf.addContentStructureElement(currentElement, markedContentCosName, standardStructureType, mc, currentPage);

        return ty;
    }

    public static String formatNumber(String value, int decimals) {
        try {
            double number = Double.parseDouble(value.replace(',', '.'));
            String formatString = "%." + decimals + "f";
            return String.format(formatString, number).replace('.', ',');
        } catch (NumberFormatException e) {
            return "0,00";
        }
    }
}
