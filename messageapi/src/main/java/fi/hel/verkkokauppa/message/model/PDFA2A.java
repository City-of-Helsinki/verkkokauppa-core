package fi.hel.verkkokauppa.message.model;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDNumberTreeNode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDArtifactMarkedContent;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.StandardStructureTypes;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationText;
import org.apache.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.XMPSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.XmpSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.xml.transform.TransformerException;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PDFA2A {
    private PDDocument pdf;
    private int mcid;
    private int currentStructureParent;

    private COSArray nums;
    private COSArray numDictionaries;


    public PDStructureElement addDocumentStructureElement() {
        PDStructureElement element = new PDStructureElement(StandardStructureTypes.DOCUMENT, null);
        element.setAlternateDescription("The document's root structure element.");
        element.setTitle("PDF Document");
        this.pdf.getDocumentCatalog().getStructureTreeRoot().appendKid(element);
        return element;
    }

    public PDStructureElement addStructureElement(PDStructureElement parent, String type) {
        PDStructureElement element = new PDStructureElement(type, parent);
        parent.appendKid(element);
        return element;
    }

    public PDStructureElement addContentStructureElement(PDStructureElement parent, COSName tag, String type, COSDictionary markedContent, PDPage page) {
        PDStructureElement element = addStructureElement(parent, type);
        element.setPage(page);
        if (tag.equals(COSName.ARTIFACT)) {
            element.appendKid(new PDArtifactMarkedContent(markedContent));
        } else {
            element.appendKid(new PDMarkedContent(tag, markedContent));
        }
        this.addNumDictionary(this.mcid - 1, tag, page, parent);
        return element;
    }

    public PDResources createFontResources(PDType0Font font, String name) {
        PDResources resources = new PDResources();
        resources.put(COSName.getPDFName(name), font);
        return resources;
    }

    public PDPage addPage(PDResources resources) {
        PDPage page = new PDPage(PDRectangle.A4);
        page.setResources(resources);
        page.getCOSObject().setItem(COSName.getPDFName("Tabs"), COSName.S);
        page.getCOSObject().setItem(COSName.STRUCT_PARENTS, COSInteger.get(0));
        this.pdf.addPage(page);
        return page;
    }

    public COSDictionary beginMarkedContent(PDPageContentStream contentStream, COSName tag) throws IOException {
        COSDictionary markedContent = new COSDictionary();
        markedContent.setName("Tag", tag.getName());
        markedContent.setInt(COSName.MCID, mcid++);
        contentStream.beginMarkedContent(
                tag,
                PDPropertyList.create(markedContent)
        );
        return markedContent;
    }

    public void endMarkedContent(PDPageContentStream contentStream) throws IOException {
        contentStream.endMarkedContent();
    }

    private void addNumDictionary(int K, COSName S, PDPage PG, PDStructureElement P) {
        COSDictionary dict = new COSDictionary();
        dict.setInt(COSName.K, K);
        dict.setString(COSName.LANG, "fi");
        dict.setItem(COSName.PG, PG.getCOSObject());
        dict.setItem(COSName.P, P.getCOSObject());
        dict.setName(COSName.S, S.getName());
        this.numDictionaries.add(dict);
    }

    public PDType0Font loadFont(PDType1Font font) throws IOException {
        return PDType0Font.load(this.pdf, new PDTrueTypeFont(font.getCOSObject()).getTrueTypeFont(), true);
    }

    public void setColorProfile(InputStream profile, String identifier, String registry) throws IOException {
        PDOutputIntent intent = new PDOutputIntent(this.pdf, profile);
        intent.setInfo(identifier);
        intent.setOutputCondition(identifier);
        intent.setOutputConditionIdentifier(identifier);
        intent.setRegistryName(registry);
        this.pdf.getDocumentCatalog().addOutputIntent(intent);
    }

    public float getUpperRightX(PDPage page) { return page.getMediaBox().getUpperRightX(); }

    public float getUpperRightY(PDPage page) { return page.getMediaBox().getUpperRightY(); }

    public float getStringWidth(PDType0Font font, int fontSize, String text) throws IOException {
        return font.getStringWidth(text) / 1000 * fontSize;
    }

    public float getStringHeight(PDType0Font font, int fontSize) {
        return font.getFontDescriptor().getCapHeight() / 1000 * fontSize;
    }

    public PDPageContentStream createContentStream(PDPage page) throws IOException {
        return new PDPageContentStream(this.pdf, page, PDPageContentStream.AppendMode.APPEND, false);
    }

    public void closeContentStream(PDPageContentStream contentStream) throws IOException {
        contentStream.close();
    }

    public PDFA2A(String title) throws BadFieldValueException, TransformerException, IOException {
        this.pdf = new PDDocument();

        PDDocumentCatalog catalog = this.pdf.getDocumentCatalog();
        catalog.setLanguage("Finnish");
        catalog.setViewerPreferences(new PDViewerPreferences(new COSDictionary()));
        catalog.getViewerPreferences().setDisplayDocTitle(true);
        catalog.setStructureTreeRoot(new PDStructureTreeRoot());

        // MarkInfo
        PDMarkInfo markInfo = new PDMarkInfo();
        markInfo.setMarked(true);
        catalog.setMarkInfo(markInfo);

        // Metadata
        XMPMetadata xmp = XMPMetadata.createXMPMetadata();
        xmp.createAndAddDublinCoreSchema();
        xmp.getDublinCoreSchema().setTitle(title);
        xmp.getDublinCoreSchema().setDescription(title);
        xmp.createAndAddPDFAExtensionSchemaWithDefaultNS();
        xmp.getPDFExtensionSchema().addNamespace("http://www.aiim.org/pdfa/ns/schema#", "pdfaSchema");
        xmp.getPDFExtensionSchema().addNamespace("http://www.aiim.org/pdfa/ns/property#", "pdfaProperty");
        xmp.getPDFExtensionSchema().addNamespace("http://www.aiim.org/pdfua/ns/id/", "pdfuaid");
        XMPSchema uaSchema = new XMPSchema(XMPMetadata.createXMPMetadata(),
                "pdfaSchema", "pdfaSchema", "pdfaSchema");
        uaSchema.setTextPropertyValue("schema", "PDF/UA Universal Accessibility Schema");
        uaSchema.setTextPropertyValue("namespaceURI", "http://www.aiim.org/pdfua/ns/id/");
        uaSchema.setTextPropertyValue("prefix", "pdfuaid");
        XMPSchema uaProp = new XMPSchema(XMPMetadata.createXMPMetadata(),
                "pdfaProperty", "pdfaProperty", "pdfaProperty");
        uaProp.setTextPropertyValue("name", "part");
        uaProp.setTextPropertyValue("valueType", "Integer");
        uaProp.setTextPropertyValue("category", "internal");
        uaProp.setTextPropertyValue("description", "Indicates, which part of ISO 14289 standard is followed");
        uaSchema.addUnqualifiedSequenceValue("property", uaProp);
        xmp.getPDFExtensionSchema().addBagValue("schemas", uaSchema);
        xmp.getPDFExtensionSchema().setPrefix("pdfuaid");
        xmp.getPDFExtensionSchema().setTextPropertyValue("part", "1");
        xmp.createAndAddPDFAIdentificationSchema();
        xmp.getPDFIdentificationSchema().setPart(2);
        xmp.getPDFIdentificationSchema().setConformance("A");
        XmpSerializer serializer = new XmpSerializer();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(xmp, baos, true);
        PDMetadata metadata = new PDMetadata(pdf);
        metadata.importXMPMetadata(baos.toByteArray());
        catalog.setMetadata(metadata);

        // Init fields
        nums = new COSArray();
        this.nums.add(COSInteger.get(0));
        this.numDictionaries = new COSArray();
        this.mcid = 0;
        this.currentStructureParent = 1;
    }

    public void save(String outputFile, boolean saveTestPDF) throws IOException {
        if (this.pdf.getDocumentCatalog().getOutputIntents().size() < 1) {
            throw new Error("Document is missing color profile");
        }

        this.pdf.getDocumentCatalog().getStructureTreeRoot().setParentTreeNextKey(currentStructureParent);
        COSDictionary dict = new COSDictionary();
        nums.add(numDictionaries);
        dict.setItem(COSName.NUMS, nums);
        this.pdf.getDocumentCatalog().getStructureTreeRoot().setParentTree(new PDNumberTreeNode(dict, dict.getClass()));

        // in local turn this on to save PDF
        if( saveTestPDF ) {
            this.pdf.save(outputFile);
        }

        this.pdf.close();
    }

    public byte[] getPDFByteArray() throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // Write PDF to byte array
            this.pdf.save(outputStream);

            // Return the PDF bytes
            return outputStream.toByteArray();
        }
    }
}
