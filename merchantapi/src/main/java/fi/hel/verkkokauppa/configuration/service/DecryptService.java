package fi.hel.verkkokauppa.configuration.service;

import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DecryptService {
    public String decryptSecret(String salt, String encryptedSecret) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(salt);
        return encryptor.decrypt(encryptedSecret);
    }
}
