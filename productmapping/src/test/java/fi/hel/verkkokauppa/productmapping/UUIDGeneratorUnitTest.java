package fi.hel.verkkokauppa.productmapping;

import org.junit.jupiter.api.Test;

import fi.hel.verkkokauppa.utils.UUIDGenerator;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UUIDGeneratorUnitTest {

    private static final String NAMESPACE1_NAME = "kaupunkiyhteinen";
    private static final String NAMESPACE1_UUID = "8f069aff-765b-3a26-92c5-1eb05ffd6388";
    private static final String NAMESPACE1_COMBINATION_EXPECTED_RESULT = "81a5e6b4-5ee2-3bb9-803e-49eb5f33d405";

    private static final String NAMESPACE2 = "tilavaraus";
    private static final String NAME2 = "123456789";


    @Test
    public void version_3_UUID_is_correctly_generated_for_domain_kaupunkiyhteinen() throws UnsupportedEncodingException {

        UUID uuid = UUIDGenerator.generateType3UUID(NAMESPACE1_UUID, NAMESPACE1_NAME);

        assertEquals(NAMESPACE1_COMBINATION_EXPECTED_RESULT, uuid.toString());
        assertEquals(3, uuid.version());
        assertEquals(2, uuid.variant());
    }

    @Test
    public void version_3_UUID_generation_is_repeatable() throws UnsupportedEncodingException {

        String first_generated_uuid = UUIDGenerator.generateType3UUIDString(NAMESPACE2, NAME2);
        String next_generated_uuid = UUIDGenerator.generateType3UUIDString(NAMESPACE2, NAME2);

        assertEquals(first_generated_uuid, next_generated_uuid);
    }

    @Test
    public void version_4_UUID_is_generated_with_correct_length_version_and_variant() {

        UUID uuid = UUIDGenerator.generateType4UUID();

        assertEquals(36, uuid.toString().length());
        assertEquals(4, uuid.version());
        assertEquals(2, uuid.variant());
    }

}