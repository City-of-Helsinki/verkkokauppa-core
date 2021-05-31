package fi.helshop.microservice.recurringorder.configuration;

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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

@Configuration
public class ElasticsearchRestClientConfig extends AbstractElasticsearchConfiguration {
            
    private final Logger log = LoggerFactory.getLogger(ElasticsearchRestClientConfig.class);

    @Autowired
    private Environment env;

    @Override
    @Bean
    public RestHighLevelClient elasticsearchClient() {
        ClientConfiguration clientConfiguration = null;

        try {
            // Elasticsearch instance requires use of ssl, but has a self-signed certificate. 
            // Blindly accept any certificate from any hostname without verifying CA.
            SSLContextBuilder sslBuilder = SSLContexts.custom()
                .loadTrustMaterial(null, (x509Certificates, s) -> true);
            final SSLContext sslContext = sslBuilder.build();

            final HostnameVerifier hostnameVerifier = new HostnameVerifier(){ 
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                };                
            };

            clientConfiguration = ClientConfiguration.builder()
                .connectedTo(env.getRequiredProperty("elasticsearch.service.url"))
                .usingSsl(sslContext, hostnameVerifier)
                .withBasicAuth(env.getRequiredProperty("elasticsearch.service.user"), env.getRequiredProperty("elasticsearch.service.password"))
                .build();

            return RestClients.create(clientConfiguration).rest();
        } catch (Exception e) {
            log.error("elasticsearch rest client initialization failed", e);
        }

        return null;
    }
}