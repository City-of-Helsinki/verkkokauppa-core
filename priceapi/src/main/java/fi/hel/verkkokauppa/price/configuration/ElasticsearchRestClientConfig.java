package fi.hel.verkkokauppa.price.configuration;

import fi.hel.verkkokauppa.common.elastic.ElasticSearchRestClientResolver;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;

@Configuration
public class ElasticsearchRestClientConfig extends AbstractElasticsearchConfiguration {

    private ElasticSearchRestClientResolver elasticSearchRestClientResolver;

    public ElasticsearchRestClientConfig(@Autowired ElasticSearchRestClientResolver elasticResolver) {
        this.elasticSearchRestClientResolver = elasticResolver;
    }

    @Override
    @Bean
    public RestHighLevelClient elasticsearchClient() {
       return this.elasticSearchRestClientResolver.get();
    }
}