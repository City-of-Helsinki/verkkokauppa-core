package fi.hel.verkkokauppa.configuration.api.namespace;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.configuration.api.namespace.dto.NamespaceDto;
import fi.hel.verkkokauppa.configuration.model.ConfigurationModel;
import fi.hel.verkkokauppa.configuration.model.LocaleModel;
import fi.hel.verkkokauppa.configuration.repository.NamespaceRepository;
import fi.hel.verkkokauppa.configuration.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@Slf4j
public class NamespaceControllerTest {
    private ArrayList<String> toBeDeleted = new ArrayList<>();
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NamespaceController namespaceController;
    @Autowired
    private NamespaceRepository namespaceRepository;

    @AfterEach
    void tearDown() {
        try {
            toBeDeleted.forEach(s -> namespaceRepository.deleteById(s));
            // Clear list because all deleted
            toBeDeleted = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
        }

    }

    /**
     * It tests the upsert merchant endpoint.
     */
    @Test
    @RunIfProfile(profile = "local")
    public void returns200WhenGetTest() throws Exception {
        String namespace = "test-namespace" + UUIDGenerator.generateType4UUID();
        NamespaceDto namespaceDto = createNamespace(namespace);
        // minnor norfgkjkgj
        namespaceDto.setCreatedAt(null);

        NamespaceDto response = namespaceController.getNamespaceDto(namespaceDto.getNamespace()).getBody();
        response.setCreatedAt(null);
        toBeDeleted.add(namespaceDto.getNamespaceId());
        Assertions.assertEquals(objectMapper.writeValueAsString(namespaceDto),objectMapper.writeValueAsString(response));

    }


    @Test
    @RunIfProfile(profile = "local")
    public void throwsError400IfEmptyNamespace() throws Exception {
        NamespaceDto namespaceDto = new NamespaceDto();

        ConfigurationModel configurationModel = new ConfigurationModel();

        ArrayList<ConfigurationModel> configurationModels = new ArrayList<>();
        configurationModels.add(configurationModel);

        namespaceDto.setConfigurations(configurationModels);

        Exception exception = assertThrows(ConstraintViolationException.class, () -> {
            AtomicReference<ResponseEntity<NamespaceDto>> response = new AtomicReference<>(namespaceController.createNamespace(namespaceDto));
            Assertions.assertEquals(response.get().getStatusCode(), HttpStatus.BAD_REQUEST);
            Assertions.assertNull(response.get().getBody());
        });
        Assertions.assertEquals(exception.getMessage(), "createNamespace.namespaceDto.namespace: namespace cant be blank");

    }

    @Test
    @RunIfProfile(profile = "local")
    public void createNamespaceWithValidDataReturnsStatus200() throws Exception {
        NamespaceDto namespaceDto = new NamespaceDto();
        String namespace = "test-namespace" + UUIDGenerator.generateType4UUID();
        namespaceDto.setNamespace(namespace);

        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setKey("test-key");
        configurationModel.setValue("test-value");

        LocaleModel locale = new LocaleModel();
        locale.setFi("test-fi");
        locale.setSv("test-sv");
        locale.setEn("test-en");
        configurationModel.setLocale(locale);

        ArrayList<ConfigurationModel> configurationModels = new ArrayList<>();
        configurationModels.add(configurationModel);

        namespaceDto.setConfigurations(configurationModels);

        ResponseEntity<NamespaceDto> response = namespaceController.createNamespace(namespaceDto);

        NamespaceDto responseNamespaceDto = response.getBody();
        Assertions.assertNotNull(responseNamespaceDto);
        String namespaceId = responseNamespaceDto.getNamespaceId();
        toBeDeleted.add(namespaceId);
        Assertions.assertNotNull(namespaceId);
        Assertions.assertNotNull(responseNamespaceDto.getCreatedAt());
        Assertions.assertNotNull(responseNamespaceDto.getConfigurations());
        Assertions.assertEquals(1, responseNamespaceDto.getConfigurations().size());

        Assertions.assertEquals(namespace, responseNamespaceDto.getNamespace());
        Assertions.assertEquals("test-key", responseNamespaceDto.getConfigurations().get(0).getKey());
        Assertions.assertEquals("test-value", responseNamespaceDto.getConfigurations().get(0).getValue());
        Assertions.assertEquals("test-fi", responseNamespaceDto.getConfigurations().get(0).getLocale().getFi());
        Assertions.assertEquals("test-sv", responseNamespaceDto.getConfigurations().get(0).getLocale().getSv());
        Assertions.assertEquals("test-en", responseNamespaceDto.getConfigurations().get(0).getLocale().getEn());
    }
    @Test
    @RunIfProfile(profile = "local")
    public void namespaceGetKeys() {
        ResponseEntity<List<String>> responseNamespaceKeys = namespaceController.getKeys();
        List<String> allKeys = new ArrayList<>(new ArrayList<>() {{
            add("merchantTermsOfServiceUrl");
            add("orderRightOfPurchaseIsActive");
            add("orderRightOfPurchaseUrl");
            add("subscriptionPriceUrl");
            add("merchantPaymentWebhookUrl");
            add("merchantOrderWebhookUrl");
            add("merchantSubscriptionWebhookUrl");
            add("namespaceApiAccessToken");
        }});
        Assertions.assertEquals(allKeys, responseNamespaceKeys.getBody());
    }

    @Test
    @RunIfProfile(profile = "local")
    public void updateValues() {
        String namespace = "test-namespace" + UUIDGenerator.generateType4UUID();
        NamespaceDto createdNamespace = createNamespace(namespace);

        String value = "new-value";
        String fi = "test-fin-change";
        createdNamespace.getConfigurations().get(0).setValue(value);
        createdNamespace.getConfigurations().get(0).getLocale().setFi(fi);
        NamespaceDto updatedNamespace = namespaceController.updateNamespaceConfigurations(createdNamespace).getBody();
        if (updatedNamespace != null) {
            Assertions.assertEquals(createdNamespace.getNamespaceId(), updatedNamespace.getNamespaceId());
            Assertions.assertEquals(createdNamespace.getNamespace(), updatedNamespace.getNamespace());
            Assertions.assertEquals(createdNamespace.getConfigurations(),updatedNamespace.getConfigurations());
        }
    }

    public NamespaceDto createNamespace(String namespace){
        NamespaceDto namespaceDto = new NamespaceDto();
        namespaceDto.setNamespace(namespace);

        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setKey("test-key");
        configurationModel.setValue("test-value");

        LocaleModel locale = new LocaleModel();
        locale.setFi("test-fi");
        locale.setSv("test-sv");
        locale.setEn("test-en");
        configurationModel.setLocale(locale);

        ArrayList<ConfigurationModel> configurationModels = new ArrayList<>();
        configurationModels.add(configurationModel);

        namespaceDto.setConfigurations(configurationModels);

        ResponseEntity<NamespaceDto> response = namespaceController.createNamespace(namespaceDto);
        return response.getBody();
    }

}
