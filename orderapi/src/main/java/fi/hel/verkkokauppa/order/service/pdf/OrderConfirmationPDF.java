package fi.hel.verkkokauppa.order.service.pdf;


import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemMetaDto;
import fi.hel.verkkokauppa.order.model.pdf.GenerateOrderConfirmationPDFRequestDto;
import fi.hel.verkkokauppa.order.model.pdf.PDFA2A;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.StandardStructureTypes;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.xmpbox.type.BadFieldValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.transform.TransformerException;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

@Component
public class OrderConfirmationPDF {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmationPDF.class);
    private final int SIDE_MARGIN = 120;
    private final int BOTTOM_MARGIN = 50;
    private final int UPPER_MARGIN = 50;
    private final int COLUMN_GAP = 10;
    private final String TITLE = "Tilausvahvistus ja kuitti";

    private final int FONT_SIZE = 12;

    private final int LINE_SPACING = 10;


    @Value("${test.pdf.save:false}")
    private boolean saveTestPDF;

    public byte[] generateNone(String outputFile, GenerateOrderConfirmationPDFRequestDto dto){
        return null;
    }

    public byte[] generate(String outputFile, GenerateOrderConfirmationPDFRequestDto dto) throws IOException, TransformerException, BadFieldValueException {

        PDFA2A pdf = new PDFA2A(TITLE);

        OrderConfirmationPDFContext pdfContext = new OrderConfirmationPDFContext(pdf);

        PDType0Font font = pdfContext.getPdf().loadFont(PDType1Font.HELVETICA);
        PDType0Font boldFont = pdfContext.getPdf().loadFont(PDType1Font.HELVETICA_BOLD);
        pdfContext.getPdf().setColorProfile(OrderConfirmationPDF.class.getResourceAsStream("/sRGB2014.icc"), "sRGB IEC61966-2.1", "http://www.color.org");

        PDStructureElement currentElement = pdfContext.getPdf().addDocumentStructureElement();
        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.PART);
        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.SECT);

        pdfContext.setCurrentPage(pdfContext.getPdf().addPage(pdfContext.getPdf().createFontResources(font, "Helv")));

        float y = pdfContext.getPdf().getUpperRightY(pdfContext.getCurrentPage()) - pdfContext.getPdf().getStringHeight(font, 20) - 25;
        float maxWidth = pdfContext.getPdf().getUpperRightX(pdfContext.getCurrentPage()) - SIDE_MARGIN * 2;

        pdfContext.setContentStream(pdfContext.getPdf().createContentStream(pdfContext.getCurrentPage()));

        COSDictionary mc;

        // Payment date and time
        // Date in format 01.01.2021, time in format 18:00
        LocalDateTime paymentDateTime = LocalDateTime.parse(dto.getPayment().getCreatedAt());
        ZonedDateTime paymentDateTimeUTC = paymentDateTime.atZone(ZoneOffset.UTC);
        ZonedDateTime paymentDateTimeEET = paymentDateTimeUTC.withZoneSameInstant(ZoneId.of("Europe/Helsinki"));
        String paymentDate = paymentDateTimeEET.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        String paymentTime = paymentDateTimeEET.format(DateTimeFormatter.ofPattern("HH:mm"));

        //
        // RECEIPT HEADERS
        //
        y = addContentElement(currentElement,
                pdfContext,
                COSName.H, StandardStructureTypes.H,
                boldFont,
                20,
                SIDE_MARGIN,
                y,
                TITLE);


        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                String.format("Kiitos tilauksestasi %s ", dto.getOrderId()));

        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                String.format("%s Tilausaika %s", paymentDate, paymentTime));


//        currentElement = pdf.addStructureElement(currentElement, StandardStructureTypes.TABLE);
        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.H2);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.H, StandardStructureTypes.H2,
                boldFont,
                16,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(boldFont, 16) + LINE_SPACING + 25),
                "Maksutiedot");

        for (OrderItemDto item : dto.getItems()) {

            // RECEIPT ITEMS

            if (item.getProductLabel() != null) {
                List<String> productLabelLines = wrapLines("", item.getProductLabel(), maxWidth, font, 16);
                for (String productLabel : productLabelLines) {
                    currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
                    y = addContentElement(currentElement,
                            pdfContext,
                            COSName.P, StandardStructureTypes.P,
                            font,
                            16,
                            SIDE_MARGIN,
                            y -= (pdfContext.getPdf().getStringHeight(font, 16) + LINE_SPACING),
                            productLabel);
                }
            }

            // define column width for product name, needed if product name requires text wrapping
            float originalPriceGrossLength = 0;
            if (item.getOriginalPriceGross() != null) {
                originalPriceGrossLength = pdfContext.getPdf().getStringWidth(font, FONT_SIZE, String.format("%s €", formatNumber(item.getOriginalPriceGross(), 2)));
            }
            float priceGrossLength = pdfContext.getPdf().getStringWidth(font, FONT_SIZE, String.format("%s € / %s", formatNumber(item.getPriceGross(),2), "kpl"));
            float rightColumnTextWidth = Math.max(originalPriceGrossLength, priceGrossLength);
            float maxWidthLeftColumn = maxWidth - rightColumnTextWidth - COLUMN_GAP;

            List<String> productNameLines = wrapLines("", item.getProductName(), maxWidthLeftColumn, boldFont, FONT_SIZE);
            for (String productName : productNameLines) {
                currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
                y = addContentElement(currentElement,
                        pdfContext,
                        COSName.P, StandardStructureTypes.P,
                        boldFont,
                        FONT_SIZE,
                        SIDE_MARGIN,
                        y -= (pdfContext.getPdf().getStringHeight(boldFont, FONT_SIZE) + LINE_SPACING),
                        productName);
            }

            currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);

            if (item.getOriginalPriceGross() != null) {
                String originalPriceGross = String.format("%s €", formatNumber(item.getOriginalPriceGross(), 2));
                y = addContentElement(currentElement,
                        pdfContext,
                        COSName.P, StandardStructureTypes.P,
                        font,
                        FONT_SIZE,
                        pdfContext.getPdf().getUpperRightX(pdfContext.getCurrentPage()) - SIDE_MARGIN - pdfContext.getPdf().getStringWidth(font, FONT_SIZE, originalPriceGross) - 1,
                        y,
                        originalPriceGross,
                        "Alkuperäinen bruttohinta");

                mc = pdfContext.getPdf().beginMarkedContent(pdfContext.getContentStream(), COSName.ARTIFACT);
                pdfContext.getContentStream().moveTo(pdfContext.getPdf().getUpperRightX(pdfContext.getCurrentPage()) - SIDE_MARGIN - pdfContext.getPdf().getStringWidth(font, FONT_SIZE, originalPriceGross) - 2, y + pdfContext.getPdf().getStringHeight(font, FONT_SIZE) / 2);
                pdfContext.getContentStream().lineTo(pdfContext.getPdf().getUpperRightX(pdfContext.getCurrentPage()) - SIDE_MARGIN, y + pdfContext.getPdf().getStringHeight(font, FONT_SIZE) / 2);
                pdfContext.getContentStream().closeAndStroke();
                pdfContext.getPdf().endMarkedContent(pdfContext.getContentStream());
                pdfContext.getPdf().addContentStructureElement(currentElement, COSName.ARTIFACT, StandardStructureTypes.P, mc, pdfContext.getCurrentPage());

                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING);
            }

            String priceGross = String.format("%s € / %s", formatNumber(item.getPriceGross(),2), "kpl");
            y = addContentElement(currentElement,
                    pdfContext,
                    COSName.P, StandardStructureTypes.P,
                    font,
                    FONT_SIZE,
                    pdfContext.getPdf().getUpperRightX(pdfContext.getCurrentPage()) - SIDE_MARGIN - pdfContext.getPdf().getStringWidth(font, FONT_SIZE, priceGross) - 1,
                    y,
                    priceGross);

            if (item.getProductDescription() != null) {
                currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.TD);
                y = addContentElement(currentElement,
                        pdfContext,
                        COSName.P, StandardStructureTypes.P,
                        font,
                        FONT_SIZE,
                        SIDE_MARGIN,
                        y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                        item.getProductDescription());
            }

            currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.TD);
            y = addContentElement(currentElement,
                    pdfContext,
                    COSName.P, StandardStructureTypes.P,
                    font,
                    FONT_SIZE,
                    SIDE_MARGIN,
                    y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                    String.format("%d %s yhteensä Sis. alv (%s%%)", item.getQuantity(), "kpl", formatNumber(item.getVatPercentage(),2)));

            String rowPriceTotal = String.format("%s €", formatNumber(item.getRowPriceTotal(),2));
            currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.TD);
            y = addContentElement(currentElement,
                    pdfContext,
                    COSName.P, StandardStructureTypes.P,
                    font,
                    FONT_SIZE,
                    pdfContext.getPdf().getUpperRightX(pdfContext.getCurrentPage()) - SIDE_MARGIN - pdfContext.getPdf().getStringWidth(font, FONT_SIZE, rowPriceTotal),
                    y,
                    rowPriceTotal);

            // Metadata
            for (OrderItemMetaDto meta : item.getMeta()) {
                if( meta.getVisibleInCheckout() != null && meta.getVisibleInCheckout().equalsIgnoreCase("true") ){
                    if( meta.getLabel() != null ) {
                        List<String> metaLabelLines = wrapLines("", meta.getLabel(), maxWidth, font, FONT_SIZE);
                        for (String metaLabel : metaLabelLines) {
                            currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
                            y = addContentElement(currentElement,
                                    pdfContext,
                                    COSName.P, StandardStructureTypes.P,
                                    font,
                                    FONT_SIZE,
                                    SIDE_MARGIN,
                                    y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                                    metaLabel);
                        }
                    }
                    if( meta.getValue() != null ) {
                        List<String> metaValueLines = wrapLines("", meta.getValue(), maxWidth, font, FONT_SIZE);
                        for (String metaValue : metaValueLines) {
                            currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
                            y = addContentElement(currentElement,
                                    pdfContext,
                                    COSName.P, StandardStructureTypes.P,
                                    font,
                                    FONT_SIZE,
                                    SIDE_MARGIN,
                                    y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                                    metaValue);
                        }
                    }
                }
            }

            y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING);

        }

        //
        // TOTAL PRICE AND PAYMENT INFO
        //
        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                "Maksettu yhteensä");

        String priceTotal = String.format("%s €", formatNumber(dto.getPayment().getTotal().toString(),2));
        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                pdfContext.getPdf().getUpperRightX(pdfContext.getCurrentPage()) - SIDE_MARGIN - pdfContext.getPdf().getStringWidth(font, FONT_SIZE, priceTotal),
                y,
                priceTotal);

        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                String.format("Sis. alv (%s%%)", formatNumber(dto.getItems().get(0).getVatPercentage(),2)));

        String alvTotal = String.format("%s €", formatNumber(dto.getPayment().getTaxAmount().toString(), 2));
        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                pdfContext.getPdf().getUpperRightX(pdfContext.getCurrentPage()) - SIDE_MARGIN - pdfContext.getPdf().getStringWidth(font, FONT_SIZE, alvTotal),
                y,
                alvTotal);

        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                "Maksutapa");

        // use paymentMethodLabel as payment method ("Maksutapa") if available, and if not, use paymentMethod
        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        if (dto.getPayment().getPaymentMethodLabel() != null && !dto.getPayment().getPaymentMethodLabel().isEmpty()) {
            y = addContentElement(currentElement,
                    pdfContext,
                    COSName.P, StandardStructureTypes.P,
                    font,
                    FONT_SIZE,
                    pdfContext.getPdf().getUpperRightX(pdfContext.getCurrentPage()) - SIDE_MARGIN - pdfContext.getPdf().getStringWidth(font, FONT_SIZE, dto.getPayment().getPaymentMethodLabel()),
                    y,
                    dto.getPayment().getPaymentMethodLabel());
        } else {
            y = addContentElement(currentElement,
                    pdfContext,
                    COSName.P, StandardStructureTypes.P,
                    font,
                    FONT_SIZE,
                    pdfContext.getPdf().getUpperRightX(pdfContext.getCurrentPage()) - SIDE_MARGIN - pdfContext.getPdf().getStringWidth(font, FONT_SIZE, dto.getPayment().getPaymentMethod()),
                    y,
                    dto.getPayment().getPaymentMethod());
        }

        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= ((pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING)*3/2 ),
                "Päivämäärä");

        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                pdfContext.getPdf().getUpperRightX(pdfContext.getCurrentPage()) - SIDE_MARGIN - pdfContext.getPdf().getStringWidth(font, FONT_SIZE, paymentDate),
                y += ((pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING)/2),
                paymentDate);
        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                pdfContext.getPdf().getUpperRightX(pdfContext.getCurrentPage()) - SIDE_MARGIN - pdfContext.getPdf().getStringWidth(font, FONT_SIZE, paymentTime),
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                paymentTime);

        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addDivider(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING));

        // CUSTOMER INFO
        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.H2);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.H, StandardStructureTypes.H2,
                boldFont,
                16,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(boldFont, 16) + LINE_SPACING),
                "Tilaajan tiedot");

        String customerName = String.format("%s %s", dto.getCustomerFirstName(), dto.getCustomerLastName());
        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                customerName);

        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                dto.getCustomerEmail(),
                Color.blue);

        if (dto.getCustomerPhoneNumber() != null && !dto.getCustomerPhoneNumber().isEmpty()) {
            currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
            y = addContentElement(currentElement,
                    pdfContext,
                    COSName.P, StandardStructureTypes.P,
                    font,
                    FONT_SIZE,
                    SIDE_MARGIN,
                    y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                    dto.getCustomerPhoneNumber());
        }

        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addDivider(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING));

        // MERCHANT INFO
        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.H2);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.H, StandardStructureTypes.H2,
                boldFont,
                16,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(boldFont, 16) + LINE_SPACING),
                "Myyjän tiedot");

        List<String> merchantNameLines = wrapLines("", dto.getMerchantName(), maxWidth, font, FONT_SIZE);
        for (String merchantName : merchantNameLines) {
            currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
            y = addContentElement(currentElement,
                    pdfContext,
                    COSName.P, StandardStructureTypes.P,
                    font,
                    FONT_SIZE,
                    SIDE_MARGIN,
                    y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                    merchantName);
        }

        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                dto.getMerchantStreetAddress());

        String merchantAddress = String.format("%s %s", dto.getMerchantZipCode(), dto.getMerchantCity());
        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                merchantAddress);

        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                dto.getMerchantEmail(),
                Color.blue);

        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                dto.getMerchantPhoneNumber());

        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                dto.getMerchantBusinessId());

        // FOOTER
        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING + LINE_SPACING),
                "Mikäli sinulla on tilaukseen liittyen kysymyksiä, kohdistathan ne suoraan");

        List<String> merchantNameLinesFooter = wrapLines("myyjälle", dto.getMerchantName() + ".", maxWidth, font, FONT_SIZE);
        for (String merchantNameFooter : merchantNameLinesFooter) {
            currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
            y = addContentElement(currentElement,
                    pdfContext,
                    COSName.P, StandardStructureTypes.P,
                    font,
                    FONT_SIZE,
                    SIDE_MARGIN,
                    y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                    merchantNameFooter);
        }

        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING + LINE_SPACING),
                "Myyjä toimittaa tarvittaessa erikseen lisäohjeet palvelun/tuotteen käyttöön");

        currentElement = pdfContext.getPdf().addStructureElement(currentElement, StandardStructureTypes.P);
        y = addContentElement(currentElement,
                pdfContext,
                COSName.P, StandardStructureTypes.P,
                font,
                FONT_SIZE,
                SIDE_MARGIN,
                y -= (pdfContext.getPdf().getStringHeight(font, FONT_SIZE) + LINE_SPACING),
                "tai toimitukseen liittyen.");



        // CLOSE PDF
        pdfContext.getPdf().closeContentStream(pdfContext.getContentStream());

        byte[] pdfArray = pdfContext.getPdf().getPDFByteArray();
        pdfContext.getPdf().save(outputFile, saveTestPDF);

        return pdfArray;
    }

    private float addContentElement(PDStructureElement currentElement,
                                    OrderConfirmationPDFContext pdfContext,
                                    COSName markedContentCosName, String standardStructureType,
                                    PDType0Font font, float fontSize, float tx, float ty, String text) throws IOException {
        return addContentElement ( currentElement,
                pdfContext,
                markedContentCosName, standardStructureType,
                font, fontSize, tx, ty, text, null, null, true);
    }

    private float addContentElement(PDStructureElement currentElement,
                                    OrderConfirmationPDFContext pdfContext,
                                    COSName markedContentCosName, String standardStructureType,
                                    PDType0Font font, float fontSize, float tx, float ty, String text, Color colour) throws IOException {
        return addContentElement (currentElement,
                pdfContext,
                markedContentCosName, standardStructureType,
                font, fontSize, tx, ty, text, null, colour, true);
    }

    private float addContentElement(PDStructureElement currentElement,
                                    OrderConfirmationPDFContext pdfContext,
                                    COSName markedContentCosName, String standardStructureType,
                                    PDType0Font font, float fontSize, float tx, float ty, String text, String alternateDescription) throws IOException {
        return addContentElement (currentElement,
                pdfContext,
                markedContentCosName, standardStructureType,
                font, fontSize, tx, ty, text, alternateDescription, null, true);
    }

    private float addContentElement(PDStructureElement currentElement,
                                    OrderConfirmationPDFContext pdfContext,
                                    COSName markedContentCosName, String standardStructureType,
                                    PDType0Font font, float fontSize, float tx, float ty, String text, String alternateDescription, Color colour, Boolean pageCheck) throws IOException {
        if( pageCheck && ty <= BOTTOM_MARGIN ){
            pdfContext.setCurrentPage(pdfContext.getPdf().addPage(pdfContext.getPdf().createFontResources(font, "Helv")));

            pdfContext.getContentStream().close();
            pdfContext.setContentStream(pdfContext.getPdf().createContentStream(pdfContext.getCurrentPage()));

            ty = pdfContext.getPdf().getUpperRightY(pdfContext.getCurrentPage()) - pdfContext.getPdf().getStringHeight(font, 20) - 25;
        }
        if( text == null ){
            return ty;
        }
        COSDictionary mc = pdfContext.getPdf().beginMarkedContent(pdfContext.getContentStream(), markedContentCosName);
        pdfContext.getContentStream().beginText();
        pdfContext.getContentStream().setFont(font, fontSize);
        pdfContext.getContentStream().newLineAtOffset(tx, ty);

        if( colour != null ) {
            pdfContext.getContentStream().setNonStrokingColor(colour);
        }
        else {
            pdfContext.getContentStream().setNonStrokingColor(Color.black);
        }

        pdfContext.getContentStream().showText(text);
        pdfContext.getContentStream().endText();
        pdfContext.getPdf().endMarkedContent(pdfContext.getContentStream());
        if( alternateDescription != null ) {
            pdfContext.getPdf().addContentStructureElement(currentElement, markedContentCosName, standardStructureType, mc, pdfContext.getCurrentPage()).setAlternateDescription(alternateDescription);
        } else{
            pdfContext.getPdf().addContentStructureElement(currentElement, markedContentCosName, standardStructureType, mc, pdfContext.getCurrentPage());
        }

        return ty;
    }

    private float addLink(PDStructureElement currentElement,
                          OrderConfirmationPDFContext pdfContext,
                          COSName markedContentCosName, String standardStructureType,
                                   PDType0Font font, float fontSize, float tx, float ty, String text, String linkUrl) throws IOException {
        if( ty <= BOTTOM_MARGIN ){
            pdfContext.setCurrentPage(pdfContext.getPdf().addPage(pdfContext.getPdf().createFontResources(font, "Helv")));

            pdfContext.getContentStream().close();
            pdfContext.setContentStream(pdfContext.getPdf().createContentStream(pdfContext.getCurrentPage()));

            ty = pdfContext.getPdf().getUpperRightY(pdfContext.getCurrentPage()) - pdfContext.getPdf().getStringHeight(font, 20) - 25;
        }
        if( text == null ){
            return ty;
        }
        COSDictionary mc = pdfContext.getPdf().beginMarkedContent(pdfContext.getContentStream(), markedContentCosName);
        pdfContext.getContentStream().beginText();
        pdfContext.getContentStream().setFont(font, fontSize);
        pdfContext.getContentStream().setNonStrokingColor(Color.blue);
        pdfContext.getContentStream().newLineAtOffset(tx, ty);
        pdfContext.getContentStream().showText(text);
        pdfContext.getContentStream().endText();

        float textWidth = pdfContext.getPdf().getStringWidth(font, FONT_SIZE, text);

        // Draw the underline
        float underlineY = ty - 2; // Slightly below text
        pdfContext.getContentStream().setLineWidth(1);
        pdfContext.getContentStream().moveTo(tx, underlineY);
        pdfContext.getContentStream().lineTo(tx +  textWidth, underlineY);
        pdfContext.getContentStream().stroke();

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

        pdfContext.getCurrentPage().getAnnotations().add(link);

        pdfContext.getPdf().endMarkedContent(pdfContext.getContentStream());
        pdfContext.getPdf().addContentStructureElement(currentElement, markedContentCosName, standardStructureType, mc, pdfContext.getCurrentPage());

        return ty;
    }

    private float addDivider(PDStructureElement currentElement,
                             OrderConfirmationPDFContext pdfContext,
                             COSName markedContentCosName, String standardStructureType,
                             PDType0Font font, float fontSize, float ty) throws IOException {
        if( ty <= BOTTOM_MARGIN ){
            pdfContext.setCurrentPage(pdfContext.getPdf().addPage(pdfContext.getPdf().createFontResources(font, "Helv")));

            pdfContext.getContentStream().close();
            pdfContext.setContentStream(pdfContext.getPdf().createContentStream(pdfContext.getCurrentPage()));
            pdfContext.getContentStream().setFont(font, fontSize);

            ty = pdfContext.getPdf().getUpperRightY(pdfContext.getCurrentPage()) - pdfContext.getPdf().getStringHeight(font, 20) - 25;
        }
        COSDictionary mc = pdfContext.getPdf().beginMarkedContent(pdfContext.getContentStream(), markedContentCosName);
        pdfContext.getContentStream().setFont(font, fontSize);
        pdfContext.getContentStream().setLineWidth(1);
        pdfContext.getContentStream().moveTo(SIDE_MARGIN-15, ty);
        pdfContext.getContentStream().lineTo(pdfContext.getPdf().getUpperRightX(pdfContext.getCurrentPage()) - (SIDE_MARGIN-15), ty);
        pdfContext.getContentStream().stroke();
        pdfContext.getPdf().endMarkedContent(pdfContext.getContentStream());
        pdfContext.getPdf().addContentStructureElement(currentElement, markedContentCosName, standardStructureType, mc, pdfContext.getCurrentPage());

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

    public static List<String> wrapLines(String initialText, String text, float maxWidth, PDType0Font font, float fontSize)
            throws IOException {

        List<String> wrappedLines = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return wrappedLines;
        }

        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        currentLine.append(initialText);

        for (String word : words) {
            String testLengthLine = currentLine.length() == 0 ? word : currentLine + " " + word;

            float textWidth = font.getStringWidth(testLengthLine) / 1000 * fontSize;

            if (textWidth > maxWidth) {
                if (currentLine.length() > 0) {
                    wrappedLines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // if a single word is too long for the line, it is added anyway to its own line
                    wrappedLines.add(word);
                    currentLine.setLength(0);
                }
            } else {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            }
        }

        if (currentLine.length() > 0) {
            wrappedLines.add(currentLine.toString());
        }

        return wrappedLines;
    }

}
