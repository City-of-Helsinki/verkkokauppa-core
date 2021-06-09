package fi.hel.verkkokauppa.common;


import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class UUIDGeneratorTest {

    @Test
    public void should_GenerateReproducibleUUIDFromGivenParameters() throws UnsupportedEncodingException {
        final UUID uuid = UUIDGenerator.generateType3UUID("8f069aff-765b-3a26-92c5-1eb05ffd6388", "kaupunkiyhteinen");

        assertEquals("81a5e6b4-5ee2-3bb9-803e-49eb5f33d405", uuid.toString());
    }

    @Test
    public void should_ThrowAssertionException_IfGivenParametersAreEmptyOrNamespaceIsNotUUIDString() throws UnsupportedEncodingException {
        assertThrows(AssertionError.class, () -> {
            UUIDGenerator.generateType3UUID("", "kaupunkiyhteinen");
            UUIDGenerator.generateType3UUID("8f069aff-765b-3a26-92c5-1eb05ffd6388", "");
            UUIDGenerator.generateType3UUID("", "");
            UUIDGenerator.generateType3UUID("this is not uuid type of string", "kaupunkiyhteinen");
        });
    }

    @Test
    public void should_GenerateReproducibleUUIDStringFromGivenParameters() {
        final String uuid = UUIDGenerator.generateType3UUIDString("8f069aff-765b-3a26-92c5-1eb05ffd6388",
                "kaupunkiyhteinen");

        assertEquals("f3f5e39d-4a8b-317d-a213-5056e0537cb7", uuid.toString());
    }
    
    @Test
    public void should_GenerateUniqueType4UUID() {
        final UUID uuid = UUIDGenerator.generateType4UUID();
        final UUID anotherUuid = UUIDGenerator.generateType4UUID();

        assertEquals(4, uuid.version());
        assertNotEquals(uuid.toString(), anotherUuid.toString());
    }



}
