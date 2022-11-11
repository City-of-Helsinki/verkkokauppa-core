package fi.hel.verkkokauppa.configuration.service;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class EncryptServiceTest {


    @Test
    public void encryptSecret() {
        EncryptService encryptService = new EncryptService();
        String secret = "test-secret";
        String encryptSecret = encryptService.encryptSecret(secret, "test-salt");
        Assertions.assertNotEquals(secret, encryptSecret);
    }
}