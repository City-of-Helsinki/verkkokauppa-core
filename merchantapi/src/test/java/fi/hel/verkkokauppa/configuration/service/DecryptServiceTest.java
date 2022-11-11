package fi.hel.verkkokauppa.configuration.service;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class DecryptServiceTest {

    @Test
    public void decryptSecret() {
        EncryptService encryptService = new EncryptService();
        String secret = "test-secret";
        String salt = "test-salt";
        String encryptSecret = encryptService.encryptSecret(secret, salt);
        // decrypt starts
        DecryptService decryptService = new DecryptService();
        String decryptSecret = decryptService.decryptSecret(salt, encryptSecret);
        Assertions.assertEquals(secret, decryptSecret);
    }
}