package fi.hel.verkkokauppa.cart.service;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
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
            SSLContextBuilder sslBuilder = SSLContexts.custom()
                .loadTrustMaterial(null, (x509Certificates, s) -> true);
            final SSLContext sslContext = sslBuilder.build();

            clientConfiguration = ClientConfiguration.builder()
                .connectedTo(env.getRequiredProperty("elasticsearch.service.url"))
                // Elasticsearch instance requires use of ssl, but has a self-signed certificate. Blindly accept it without verifying CA.
                .usingSsl(sslContext, new HostnameVerifier(){ 
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
        } catch (KeyStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        RestHighLevelClient client = RestClients.create(clientConfiguration).rest();

        return client;                         
    }
}