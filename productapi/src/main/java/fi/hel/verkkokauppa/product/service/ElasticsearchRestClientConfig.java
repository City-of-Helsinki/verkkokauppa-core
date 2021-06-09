package fi.hel.verkkokauppa.product.service;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import fi.hel.verkkokauppa.common.elastic.ElasticSearchRestClientResolver;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
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