package fi.hel.verkkokauppa.common.lock;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ResourceLock {
    @Autowired
    RestHighLevelClient elasticsearch;

    private final String indexName = "resource_lock";

    public boolean obtain(String id) throws IOException {
        try {
            this.elasticsearch.index(new IndexRequest(this.indexName).id(id).source("{}", XContentType.JSON).opType(DocWriteRequest.OpType.CREATE), RequestOptions.DEFAULT);
            return true;
        } catch (ElasticsearchStatusException e) {
            if (e.getMessage().contains("version_conflict_engine_exception")) {
                return false;
            } else {
                throw e;
            }
        } catch (VersionConflictEngineException e) {
            return false;
        }
    }

    public void release(String id) throws IOException {
        this.elasticsearch.delete(new DeleteRequest(this.indexName).id(id), RequestOptions.DEFAULT);
    }
}
