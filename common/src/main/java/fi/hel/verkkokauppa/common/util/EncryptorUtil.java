package fi.hel.verkkokauppa.common.util;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

public final class EncryptorUtil {

	public static String encryptValue(String value, String password) {
		StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
		encryptor.setPassword(password);
		return encryptor.encrypt(value);
	}

	public static String decryptValue(String value, String password) {
		StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
		encryptor.setPassword(password);
		return encryptor.decrypt(value);
	}

}
