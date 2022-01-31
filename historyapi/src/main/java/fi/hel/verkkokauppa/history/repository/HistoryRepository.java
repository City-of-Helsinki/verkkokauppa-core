package fi.hel.verkkokauppa.history.repository;


import fi.hel.verkkokauppa.history.model.HistoryModel;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistoryRepository extends ElasticsearchRepository<HistoryModel, String> {
   List<HistoryModel> findHistoryModelsByNamespaceAndEntityId(String namespace, String entityId);
   List<HistoryModel> findHistoryModelsByNamespaceAndEventType(String namespace, String entityId);
}
