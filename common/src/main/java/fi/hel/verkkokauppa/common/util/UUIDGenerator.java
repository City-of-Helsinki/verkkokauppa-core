package fi.hel.verkkokauppa.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class UUIDGenerator {

    private static Logger logger = LoggerFactory.getLogger(UUIDGenerator.class);
    public static UUID generateType3UUID(String namespace, String name) throws UnsupportedEncodingException {

        byte[] nameSpaceBytes = bytesFromUUID(namespace);
        byte[] nameBytes = name.getBytes("UTF-8");
        byte[] result = joinBytes(nameSpaceBytes, nameBytes);

        return UUID.nameUUIDFromBytes(result);
    }
    public static String generateType3UUIDString(String namespace, String name) {
        try {
            String namespaceUUID = UUIDGenerator.generateNameUUIDString(namespace);
            UUID namespacedEntityUUID = UUIDGenerator.generateType3UUID(namespaceUUID, name);
            return namespacedEntityUUID.toString();
        } catch (UnsupportedEncodingException e) {
            logger.error("type3 uuid string generation failed", e);
            return null;
        }
    }

    public static String generateNameUUIDString(String name) throws UnsupportedEncodingException {
        byte[] nameBytes = name.getBytes("UTF-8");
        UUID uuid = UUID.nameUUIDFromBytes(nameBytes);
        return uuid.toString();
    }

    public static UUID generateType4UUID() {
        UUID uuid = UUID.randomUUID();
        return uuid;
    }

    private static byte[] bytesFromUUID(String uuidHexString) {
        String normalizedUUIDHexString = uuidHexString.replace("-","");

        assert normalizedUUIDHexString.length() == 32;

        byte[] bytes = new byte[16];
        for (int i = 0; i < 16; i++) {
            byte b = hexToByte(normalizedUUIDHexString.substring(i*2, i*2+2));
            bytes[i] = b;
        }
        return bytes;
    }

    public static byte hexToByte(String hexString) {
        int firstDigit = Character.digit(hexString.charAt(0),16);
        int secondDigit = Character.digit(hexString.charAt(1),16);
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    public static byte[] joinBytes(byte[] byteArray1, byte[] byteArray2) {
        int finalLength = byteArray1.length + byteArray2.length;
        byte[] result = new byte[finalLength];

        for(int i = 0; i < byteArray1.length; i++) {
            result[i] = byteArray1[i];
        }

        for(int i = 0; i < byteArray2.length; i++) {
            result[byteArray1.length+i] = byteArray2[i];
        }

        return result;
    }

    public static void main(String[] args) {
        try {
            String namespace = "kaupunkiyhteinen";
            String nameUUID = UUIDGenerator.generateNameUUIDString(namespace);
            System.out.println("namespace: " + namespace + " UUID: " + nameUUID);

            String namespace2 = "verkkokauppa";
            String nameUUID2 = UUIDGenerator.generateNameUUIDString(namespace2);
            System.out.println("namespace: " + namespace2 + " UUID: " + nameUUID2);

            UUID uuid = UUIDGenerator.generateType3UUID("8f069aff-765b-3a26-92c5-1eb05ffd6388", "kaupunkiyhteinen");
            System.out.println("8f069aff-765b-3a26-92c5-1eb05ffd6388 + kaupunkiyhteinen, uuid: " + uuid);

        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}