package fi.hel.verkkokauppa.cart.service;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;

@Configuration
public class ElasticsearchRestClientConfig extends AbstractElasticsearchConfiguration {

    @Autowired
    private Environment env;

    @Override
    @Bean
    public RestHighLevelClient elasticsearchClient() {

        ClientConfiguration clientConfiguration = null;
        try {
            clientConfiguration = ClientConfiguration.builder()  
                .connectedTo(env.getRequiredProperty("elasticsearch.service.url"))
                // Elasticsearch instance requires use of ssl, but has a self-signed certificate. Blindly accept it without verifying CA.
                .usingSsl(SSLContext.getDefault(), new HostnameVerifier(){ 
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    };                
                })
                .build();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return RestClients.create(clientConfiguration).rest();                         
    }
}