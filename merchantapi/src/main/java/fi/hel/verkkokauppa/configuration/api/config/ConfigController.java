package fi.hel.verkkokauppa.configuration.api.config;

import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
public class ConfigController {

    @GetMapping("/config/keys/getAll")
    public ResponseEntity<Map<String, List<String>>> getAllConfigurationKeys() {
        Map<String, List<String>> configurationMap = new HashMap<>();

        configurationMap.put("platform", ServiceConfigurationKeys.getPlatformKeys());
        configurationMap.put("namespace", ServiceConfigurationKeys.getNamespaceKeys());
        configurationMap.put("merchant", ServiceConfigurationKeys.getMerchantKeys());
        configurationMap.put("merchant-can-override", ServiceConfigurationKeys.getOverridableMerchantKeys());

        return ResponseEntity.ok(configurationMap);
    }

}
