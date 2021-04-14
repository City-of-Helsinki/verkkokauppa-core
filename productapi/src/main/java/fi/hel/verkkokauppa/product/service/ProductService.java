package fi.hel.verkkokauppa.product.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import fi.hel.verkkokauppa.product.model.Product;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

@Component
public class ProductService {
    
    private Logger log = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private Environment env;


    public Product findById(String productId) {
        // initialize http client
        WebClient client = getClient();

        // query product mapping from common product mapping service
        JSONObject productMapping = queryJsonService(client, env.getProperty("productmapping.url")+productId);

        String namespace = (String) productMapping.get("namespace");
        String namespaceEntityId = (String) productMapping.get("namespaceEntityId");
        log.debug("namespace: " + namespace + " namespaceEntityId: " + namespaceEntityId);

        // resolve original product backend from common service mapping service        
        String serviceUrl = resolveServiceUrl(client, namespace, "product");

        // query product data from origin backend service
        JSONObject originalProduct = queryJsonService(client, serviceUrl+namespaceEntityId);
        String productName = (String) originalProduct.get("name");

        // construct a common product with mapping and original content
        Product product = new Product(productId, productName, productMapping, originalProduct);
        log.debug("product: " + product);

        return product;
    }

    private String resolveServiceUrl(WebClient client, String namespace, String serviceType) {
        String serviceMappingUrl = env.getProperty("servicemapping.url")+"?t="+serviceType+"&n="+namespace;
        JSONObject serviceMapping = queryJsonService(client, serviceMappingUrl);

        String serviceUrl = (String) serviceMapping.get("serviceUrl");
        log.debug("serviceUrl: " + serviceUrl);

        return serviceUrl;
    }

    private WebClient getClient() {
        // expect a response within a few seconds
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
            .responseTimeout(Duration.ofMillis(3000))
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler(3000, TimeUnit.MILLISECONDS))
                .addHandlerLast(new WriteTimeoutHandler(3000, TimeUnit.MILLISECONDS)));

        WebClient client = WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();

        return client;
    }

    private JSONObject queryJsonService(WebClient client, String url) {
        String jsonResponse = client.get()
            .uri(url)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        JSONObject jsonObject = new JSONObject(jsonResponse);

        return jsonObject;
    }

}
