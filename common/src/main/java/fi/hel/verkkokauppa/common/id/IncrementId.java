package fi.hel.verkkokauppa.common.id;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class IncrementId {
  @Autowired
  RestHighLevelClient elasticsearch;

  private final String indexName = "increment_ids";

  private Long generateIncrementId(String series) throws IOException {
    IndexResponse res = this.elasticsearch.index(new IndexRequest(this.indexName).id(series).source("{}", XContentType.JSON), RequestOptions.DEFAULT);
    return res.getVersion();
  }

  public Long generateOrderIncrementId() throws IOException {
    return this.generateIncrementId("order");
  }
}
