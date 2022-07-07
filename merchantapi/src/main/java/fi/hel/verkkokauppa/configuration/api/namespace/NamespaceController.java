package fi.hel.verkkokauppa.configuration.api.namespace;

import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.configuration.api.namespace.dto.NamespaceDto;
import fi.hel.verkkokauppa.configuration.service.NamespaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@Validated
@RestController
public class NamespaceController {
    @Autowired
    private NamespaceService namespaceService;


    /**
     * > Creates namespace with configurations
     *
     * @param namespaceDto The object that will be passed to the service layer.
     * @return ResponseEntity<NamespaceDto>
     */
    @PostMapping(value = "/namespace/create", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NamespaceDto> createNamespace(@RequestBody @Valid NamespaceDto namespaceDto) {
        try {
            // Returning a response with a status code of 201 (CREATED) and the body of the response is the namespaceDto.
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    namespaceService.save(namespaceDto)
            );
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-namespace", "failed to create namespace configuration, namespace:" + namespaceDto.getNamespace())
            );
        }
    }

    @PostMapping(value = "/namespace/updateValues", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NamespaceDto> updateNamespaceConfigurations(@RequestBody @Valid NamespaceDto namespaceDto) {
        try {
            // Returning a response with a status code of 200 (OK) and the body of the response is the namespaceDto.
            return ResponseEntity.status(HttpStatus.OK).body(
                    namespaceService.update(namespaceDto)
            );
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-update-namespace", "failed to update namespace configuration, namespace:" + namespaceDto.getNamespace())
            );
        }
    }

    @GetMapping("/namespace/get")
    public ResponseEntity<NamespaceDto> getNamespaceDto(@RequestParam(value = "namespace") String namespace) {
        return ResponseEntity.ok(
                namespaceService.findByNamespace(namespace)
        );
    }

    @GetMapping("/namespace/keys")
    public ResponseEntity<List<String>> getKeys() {
        return ResponseEntity.ok(ServiceConfigurationKeys.getNamespaceKeys());
    }

    @GetMapping("/namespace/getValue")
    public ResponseEntity<String> getValue(
            @RequestParam(value = "namespace") String namespace,
            @RequestParam(value = "key") String key
    ) {
        return ResponseEntity.ok(
                namespaceService.getConfigurationValueByMerchantIdAndNamespaceAndKey(namespace, key)
        );
    }

}
