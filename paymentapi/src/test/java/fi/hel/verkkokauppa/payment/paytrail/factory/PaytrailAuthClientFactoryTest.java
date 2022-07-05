package fi.hel.verkkokauppa.payment.paytrail.factory;

import org.helsinki.paytrail.PaytrailClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


class PaytrailAuthClientFactoryTest {
    @BeforeEach
    void setUp() {
        paytrailAuthClientFactory = new PaytrailAuthClientFactory();
    }

    private PaytrailAuthClientFactory paytrailAuthClientFactory;

    @Test
    void getClient() {
        PaytrailClient client = paytrailAuthClientFactory.getClient("123");
        Assertions.assertEquals(client.getBaseUrl(),"https://services.paytrail.com");
    }
}