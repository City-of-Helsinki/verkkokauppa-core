package fi.hel.verkkokauppa.common.rest;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


@Component
public class RestServiceClient {

    private Logger log = LoggerFactory.getLogger(RestServiceClient.class);

    public JSONObject makeGetCall(String url) {
        WebClient client = getClient();
        JSONObject response = queryJsonService(client, url);
        return response;
    }

    public JSONObject makePostCall(String url, String body) {
        WebClient client = getClient();
        JSONObject response = postQueryJsonService(client, url, body);
        return response;
    }

    public WebClient getClient() {
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

    public JSONObject queryJsonService(WebClient client, String url) {
        String jsonResponse = client.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONObject jsonObject = new JSONObject(jsonResponse);

        return jsonObject;
    }

    public JSONObject postQueryJsonService(WebClient client, String url, String body) {
        String jsonResponse = client.post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONObject jsonObject = new JSONObject(jsonResponse);

        return jsonObject;
    }

}

