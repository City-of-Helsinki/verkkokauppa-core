package fi.hel.verkkokauppa.events;

import fi.hel.verkkokauppa.common.constants.NamespaceType;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootTest
@Slf4j
public class EventsApplicationTest {

    @Autowired
    private CommonServiceConfigurationClient configurationClient;

    @Autowired
    private RestServiceClient restServiceClient;


//    @Test
    void getApiKeyWorks() {
        String authKey = configurationClient.getAuthKey(NamespaceType.ADMIN);
        log.info(authKey);
        Assertions.assertNotNull(authKey);
    }

//  @Test
    void getQueryWithAuthenticationWorks() {
        JSONObject response = restServiceClient.makeAdminGetCall("http://localhost:8084/v1/order/admin/e14d791f-7dbe-3688-b78e-963685c6be14");
        Assertions.assertNotNull(response);
    }
}