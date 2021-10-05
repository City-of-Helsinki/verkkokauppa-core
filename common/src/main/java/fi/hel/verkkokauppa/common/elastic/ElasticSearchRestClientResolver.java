package fi.hel.verkkokauppa.common.elastic;


import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ElasticSearchRestClientResolver {

    private Logger log = LoggerFactory.getLogger(ElasticSearchRestClientResolver.class);

    @Value("${elasticsearch.service.url}")
    private String serviceUrl;

    @Value("${elasticsearch.service.user}")
    private String username;

    @Value("${elasticsearch.service.password}")
    private String password;

    // if not set defaults to false
    @Value("${elasticsearch.service.local.environment:#{false}}")
    private Boolean isLocalEnvironment;

    public RestHighLevelClient get() {
        if (this.isLocalEnvironment) {
            return getLocalElasticSearchRestClient();
        }

        return getProductionElasticSearchRestClient();
    }

    private RestHighLevelClient getLocalElasticSearchRestClient() {
        ClientConfiguration.MaybeSecureClientConfigurationBuilder maybeSecureClientConfigurationBuilder = ClientConfiguration.builder()
                .connectedTo(this.serviceUrl);

        if (!Objects.equals(this.password, "") && !Objects.equals(this.username, "")) {
            maybeSecureClientConfigurationBuilder.withBasicAuth(this.username, this.password);
        }

        final ClientConfiguration clientConfiguration = maybeSecureClientConfigurationBuilder
                .build();

        return RestClients.create(clientConfiguration).rest();
    }

    private RestHighLevelClient getProductionElasticSearchRestClient() {
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
                    .connectedTo(this.serviceUrl)
                    .usingSsl(sslContext, hostnameVerifier)
                    .withBasicAuth(this.username, this.password)
                    .build();

        } catch (Exception e) {
            log.error("elasticsearch rest client initialization failed", e);
        }

        return RestClients.create(clientConfiguration).rest();
    }
}
