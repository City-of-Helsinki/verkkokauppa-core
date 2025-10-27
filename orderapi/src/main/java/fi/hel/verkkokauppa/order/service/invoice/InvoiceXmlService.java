package fi.hel.verkkokauppa.order.service.invoice;

import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.model.invoice.InvoicingXml;
import fi.hel.verkkokauppa.order.repository.jpa.InvoicingXmlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class InvoiceXmlService {

    private InvoicingXmlRepository invoicingXmlRepository;

    @Autowired
    public InvoiceXmlService(InvoicingXmlRepository invoicingXmlRepository) {
        this.invoicingXmlRepository = invoicingXmlRepository;
    }

    public void save(String xmlFileName, String xml){
        InvoicingXml invoicingXml = new InvoicingXml();
        LocalDate now = LocalDate.now();
        List<InvoicingXml> invoicesToday = invoicingXmlRepository.findAllByTimestamp(now);

        invoicingXml.setTimestamp(now);
        invoicingXml.setCounter(invoicesToday.size()+1); // counter for xml files done today
        invoicingXml.setXmlId(UUIDGenerator.generateType3UUIDString(xmlFileName, invoicingXml.getTimestamp().toString() + invoicingXml.getCounter()));
        invoicingXml.setXmlFileName(xmlFileName);
        invoicingXml.setXml(xml);

        invoicingXmlRepository.save(invoicingXml);
    }
}
