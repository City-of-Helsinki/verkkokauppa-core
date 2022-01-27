package fi.hel.verkkokauppa.history.repository;


import fi.hel.verkkokauppa.history.model.HistoryModel;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoryRepository extends ElasticsearchRepository<HistoryModel, String> {

}
