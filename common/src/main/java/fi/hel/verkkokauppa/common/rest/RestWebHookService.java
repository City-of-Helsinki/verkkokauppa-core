package fi.hel.verkkokauppa.common.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Getter
@Setter
@NoArgsConstructor
@Component
public class RestWebHookService {
    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private CommonServiceConfigurationClient configurations;

    @Autowired
    private ObjectMapper objectMapper;
    
    private String webHookUrl;

    public ResponseEntity<Void> postCallWebHook(Object object, String webHookConfigurationKey, String namespace) throws JsonProcessingException {

        if (namespace == null || namespace.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String webhookUrl = configurations.getPublicServiceConfigurationValue(namespace, webHookConfigurationKey);

        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // Removes null values.
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //format payload, message to json string conversion
        String body = objectMapper.writeValueAsString(object);

        restServiceClient.makeVoidPostCall(webhookUrl, body);
        return ResponseEntity.ok().build();
    }

}
