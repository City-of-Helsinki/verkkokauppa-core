package fi.hel.verkkokauppa.order.service.invoice;

import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.model.invoice.InvoicingXml;
import fi.hel.verkkokauppa.order.repository.jpa.InvoicingXmlRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

public class InvoiceXmlService {

    @Autowired
    private InvoicingXmlRepository invoicingXmlRepository;

    public void save(String xmlFileName, String xml){
        InvoicingXml invoicingXml = new InvoicingXml();

        invoicingXml.setTimestamp(LocalDate.now());
        invoicingXml.setXmlId(UUIDGenerator.generateType3UUIDString(xmlFileName, invoicingXml.getTimestamp().toString()));
        invoicingXml.setXmlFileName(xmlFileName);
        invoicingXml.setXml(xml);

        invoicingXmlRepository.save(invoicingXml);
    }
}
