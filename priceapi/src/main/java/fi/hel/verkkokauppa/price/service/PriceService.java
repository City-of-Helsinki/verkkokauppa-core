package fi.hel.verkkokauppa.price.service;

import fi.hel.verkkokauppa.price.model.Price;
import fi.hel.verkkokauppa.price.model.PriceModel;
import fi.hel.verkkokauppa.price.repository.PriceRepository;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
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
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class PriceService {

    public static final int CONNECT_TIMEOUT = 30000;
    private Logger log = LoggerFactory.getLogger(PriceService.class);

    @Autowired
    private Environment env;

    @Autowired
    private PriceRepository priceRepository;


    public Price findByCommonProductId(String productId) {
        try {
            // initialize http client
            WebClient client = getClient();

            // query product mapping from common product mapping service
            JSONObject productMapping = queryJsonService(client, env.getProperty("productmapping.url") + productId);
            String namespace = (String) productMapping.get("namespace");
            String namespaceEntityId = (String) productMapping.get("namespaceEntityId");
            log.debug("namespace: " + namespace + " namespaceEntityId: " + namespaceEntityId);

            JSONObject priceDetails = findByNamespaceEntityId(client, namespace, namespaceEntityId);

            // construct a common price response, priceDetails may vary per backend service
            String productPrice = (String) priceDetails.get("grossValue");
            Price price = new Price(productId, productPrice, priceDetails);
            log.debug("price: " + price);

            return price;
        } catch (Exception e) {
            log.error("getting price from backend failed, productId: " + productId, e);
        }

        log.debug("price not found from backend, productId: " + productId);
        return null;
    }

    // Function to calculate net value and VAT value
    public static double[] calculateNetAndVat(double grossValue, double vatPercentage) {
        // Calculate net value
        double netValue = grossValue / (1 + (vatPercentage / 100));
        // Calculate VAT value
        double vatValue = grossValue - netValue;

        // Rounding to two decimal places
        netValue = roundToTwoDecimals(netValue);
        vatValue = roundToTwoDecimals(vatValue);

        return new double[]{netValue, vatValue};
    }


    // Method to round a double value to two decimal places
    public static double roundToTwoDecimals(double value) {
        BigDecimal bd = new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public PriceModel findByCommonProductIdAndCreateInternalProduct(String productId, String internalPrice, String productVatPercentage) {
        try {
            // initialize http client
            WebClient client = getClient();

            // query product mapping from common product mapping service
            JSONObject productMapping = queryJsonService(client, env.getProperty("productmapping.url") + productId);
            String namespace = (String) productMapping.get("namespace");
            String namespaceEntityId = (String) productMapping.get("namespaceEntityId");
            log.debug("namespace: " + namespace + " namespaceEntityId: " + namespaceEntityId);

            JSONObject priceDetails = new JSONObject();
            double grossValue = Double.parseDouble(internalPrice);
            double vatPercentage = Double.parseDouble(productVatPercentage);

            double[] result = calculateNetAndVat(grossValue, vatPercentage);

            double netValue = result[0];
            double vatValue = result[1];
            priceDetails.put("grossValue", internalPrice);
            priceDetails.put("netValue", Double.toString(netValue));
            priceDetails.put("vatValue", Double.toString(vatValue));

            // construct a common price response, priceDetails may vary per backend service
            String productPrice = (String) priceDetails.get("grossValue");

            log.debug("price: " + productPrice);
            PriceModel priceModel = new PriceModel(productId, productPrice, priceDetails, productVatPercentage);
            return priceRepository.save(priceModel);
        } catch (Exception e) {
            log.error("creating internal price failed, productId: " + productId, e);
        }

        log.debug("price not found from backend, productId: " + productId);
        return null;
    }

    private JSONObject findByNamespaceEntityId(WebClient client, String namespace, String namespaceEntityId) {
        // resolve original backend from common servicemapping service
        String serviceUrl = resolveServiceUrl(client, namespace, "price");

        // query data from original backend
        JSONObject originalPrice = queryJsonService(client, serviceUrl + namespaceEntityId);

        return originalPrice;
    }

    private String resolveServiceUrl(WebClient client, String namespace, String serviceType) {
        String serviceMappingUrl = env.getProperty("servicemapping.url") + "?t=" + serviceType + "&n=" + namespace;
        JSONObject serviceMapping = queryJsonService(client, serviceMappingUrl);

        String serviceUrl = (String) serviceMapping.get("serviceUrl");
        log.debug("serviceUrl: " + serviceUrl);

        return serviceUrl;
    }

    private WebClient getClient() {
        // expect a response within a few seconds
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
                .responseTimeout(Duration.ofMillis(CONNECT_TIMEOUT))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)));

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


    public PriceModel resolvePriceByNamespaceEntityId(String namespaceEntityId) {
        try {
            // query product mapping from common product mapping service
            JSONObject productMapping = queryJsonService(this.getClient(),env.getProperty("productmapping.internal.url") + namespaceEntityId);

            String productId = (String) productMapping.get("productId");

            return this.priceRepository.findByProductId(productId);
        } catch (Exception e) {
            log.error("Fetching internal product backend failed, namespaceEntityId: " + namespaceEntityId, e);
        }

        log.debug("product not found from backend, namespaceEntityId: " + namespaceEntityId);
        return null;
    }
}

