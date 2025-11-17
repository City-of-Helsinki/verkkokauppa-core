package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.invoice.InvoicingXml;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InvoicingXmlRepository extends ElasticsearchRepository<InvoicingXml, String> {
    List<InvoicingXml> findAllByTimestamp(LocalDate timestamp);
}
