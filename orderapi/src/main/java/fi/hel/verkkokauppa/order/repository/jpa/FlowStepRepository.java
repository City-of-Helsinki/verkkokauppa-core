package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.FlowStep;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlowStepRepository extends ElasticsearchRepository<FlowStep, String> {

    List<FlowStep> findByOrderId(String orderId);
}
