package fi.hel.verkkokauppa.common.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.constants.NamespaceType;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Component
public class RestServiceClient {

    public static final int CONNECT_TIMEOUT = 30000;
    private Logger log = LoggerFactory.getLogger(RestServiceClient.class);

    @Autowired
    private CommonServiceConfigurationClient configurationClient;

    @Autowired
    private CommonServiceConfigurationClient configurations;

    @Autowired
    private ObjectMapper objectMapper;


    public JSONObject makeGetCall(String url) {
        WebClient client = getClient();
        JSONObject response = queryJsonService(client, url);
        return Objects.requireNonNullElseGet(response, JSONObject::new);
    }

    public JSONObject makeDeleteCall(String url) {
        WebClient client = getClient();
        JSONObject response = deleteJsonService(client, url);
        return Objects.requireNonNullElseGet(response, JSONObject::new);
    }

    public JSONObject makePostCall(String url, String body) {
        WebClient client = getClient();
        JSONObject response = postQueryJsonService(client, url, body);
        return Objects.requireNonNullElseGet(response, JSONObject::new);
    }

    public JSONObject makeAdminPostCall(String url, String body) {
        WebClient client = getAdminClient();
        JSONObject response = postQueryJsonService(client, url, body);
        return Objects.requireNonNullElseGet(response, JSONObject::new);
    }

    public JSONObject makeAdminGetCall(String url) {
        WebClient client = getAdminClient();
        JSONObject response = queryJsonService(client, url);
        return Objects.requireNonNullElseGet(response, JSONObject::new);
    }


    public void makeVoidPostCall(String url, String body, String namespace) {
        WebClient client = getWebhookAuthClient(namespace);
        postVoidQueryJsonService(client, url, body);
    }

    public WebClient getClient() {
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

    public WebClient getWebhookAuthClient(String namespace) {
        // expect a response within a few seconds
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
                .responseTimeout(Duration.ofMillis(CONNECT_TIMEOUT))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)));

        String apiKey = null;
        try {
            apiKey = configurationClient.getWebhookAuthKey(namespace);
        } catch (Exception e) {
            log.info("Cant fetch webhook api key for namespace " + namespace);
        }

        WebClient client = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("webhook-api-key", apiKey)
                .defaultHeader("namespace", namespace)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        return client;
    }

    public WebClient getAdminClient() {
        // expect a response within a few seconds
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
                .responseTimeout(Duration.ofMillis(CONNECT_TIMEOUT))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)));

        String apiKey = configurationClient.getAuthKey(NamespaceType.ADMIN);

        WebClient client = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("api-key", apiKey)
                .defaultHeader("namespace", NamespaceType.ADMIN)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        return client;
    }

    public JSONObject queryJsonService(WebClient client, String url) {
        String jsonResponse = client.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (jsonResponse == null) {
            return new JSONObject();
        } else {
            return new JSONObject(jsonResponse);
        }

    }

    public JSONObject deleteJsonService(WebClient client, String url) {
        String jsonResponse = client.delete()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (jsonResponse == null) {
            return new JSONObject();
        } else {
            return new JSONObject(jsonResponse);
        }

    }

    public JSONArray queryJsonArrayService(WebClient client, String url) {
        String jsonResponse = client.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (jsonResponse == null) {
            return new JSONArray();
        } else {
            return new JSONArray(jsonResponse);
        }
    }

    public String queryStringService(String url) {
        return getClient().get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public JSONObject postQueryJsonService(WebClient client, String url, String body) {
        String jsonResponse = client.post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (jsonResponse == null) {
            return new JSONObject();
        } else {
            return new JSONObject(jsonResponse);
        }
    }

    public void postVoidQueryJsonService(WebClient client, String url, String body) {
        String response = client.post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.info("Response from postVoidQueryJsonService: {}", response);
    }


    public ResponseEntity<JSONObject> postCall(Object object, String configurationKey, String namespace) throws JsonProcessingException {

        if (namespace == null || namespace.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String webhookUrl = configurations.getPublicServiceConfigurationValue(namespace, configurationKey);

        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // Removes null values.
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //format payload, message to json string conversion
        String body = objectMapper.writeValueAsString(object);
        log.info("request body : {}", object);
        JSONObject jsonResponse =  this.makePostCall(webhookUrl, body);
        return ResponseEntity.ok().body(jsonResponse);
    }
}

