package fi.hel.verkkokauppa.configuration.api.merchant;

import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.configuration.api.merchant.dto.ConfigurationDto;
import fi.hel.verkkokauppa.configuration.api.merchant.dto.MerchantDto;
import fi.hel.verkkokauppa.configuration.api.merchant.dto.PaytrailMerchantMappingDto;
import fi.hel.verkkokauppa.configuration.model.ConfigurationModel;
import fi.hel.verkkokauppa.configuration.service.DecryptService;
import fi.hel.verkkokauppa.configuration.service.EncryptService;
import fi.hel.verkkokauppa.configuration.service.MerchantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import static fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys.MERCHANT_PAYTRAIL_MERCHANT_ID;

@Slf4j
@Validated
@RestController
public class MerchantController {
    private final MerchantService merchantService;

    private final Environment env;
    private final EncryptService encryptService;
    private final DecryptService decryptService;

    @Autowired
    public MerchantController(MerchantService merchantService, Environment env, EncryptService encryptService, DecryptService decryptService) {
        this.merchantService = merchantService;
        this.env = env;
        this.encryptService = encryptService;
        this.decryptService = decryptService;
    }

    /**
     * > Upserts a merchant
     *
     * @param merchantDto The object that will be passed to the service layer.
     * @return ResponseEntity<NamespaceDto>
     */
    @PostMapping(value = "/merchant/upsert", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MerchantDto> upsertMerchant(@RequestBody @Valid MerchantDto merchantDto) {
        try {
            MerchantDto returnDto;

            // Checking if the merchant already exists. If it does, it will update the merchant.
            if (merchantDto.getMerchantId() != null) {
                returnDto = merchantService.update(merchantDto);
            } else {
                returnDto = merchantService.save(merchantDto);
            }

            // search configurations for paytrail merchant id
            String merchantPaytrailMerchantId = null;
            if(returnDto.getConfigurations() != null && !returnDto.getConfigurations().isEmpty()){
                for(ConfigurationModel merchantConfiguration : returnDto.getConfigurations()){
                    if(merchantConfiguration.getKey() != null && merchantConfiguration.getKey().equals(MERCHANT_PAYTRAIL_MERCHANT_ID)){
                        merchantPaytrailMerchantId = merchantConfiguration.getValue();
                        break;
                    }
                }
            }

            if( merchantPaytrailMerchantId == null ){
                // check merchant entity for paytrail merchant id
                merchantPaytrailMerchantId = returnDto.getMerchantPaytrailMerchantId();
            }

            if (merchantPaytrailMerchantId != null) {
                PaytrailMerchantMappingDto paytrailMerchantMappingDto = merchantService.getPaytrailMerchantMappingByMerchantPaytrailMerchantIdAndNamespace(merchantPaytrailMerchantId, returnDto.getNamespace());
                if (paytrailMerchantMappingDto != null) {
                    addPaytrailSecret(returnDto.getMerchantId(), paytrailMerchantMappingDto.getMerchantPaytrailSecret());
                } else {
                    String paytrailSecret = getPaytrailSecret(returnDto.getMerchantId()).getBody();
                    if (paytrailSecret == null || paytrailSecret.isEmpty()) {
                        throw new CommonApiException(
                                HttpStatus.NOT_FOUND,
                                new Error("paytrail-merchant-mapping-not-found", "paytrail merchant mapping with namespace, merchantPaytrailMerchantId: [" + merchantDto.getNamespace() + ", " + merchantPaytrailMerchantId + "] not found")
                        );
                    }
                }
            }

            // Returning a response with a status code of 201 (CREATED) and the body of the response is the
            // merchantDto.
            return ResponseEntity.status(HttpStatus.CREATED).body(returnDto);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-upsert-merchant", "failed to upsert merchant, merchantId:" + merchantDto.getMerchantId())
            );
        }
    }

    @GetMapping("/merchant/keys")
    public ResponseEntity<List<String>> getKeys() {
        return ResponseEntity.ok(ServiceConfigurationKeys.getMerchantKeys());
    }

    @GetMapping("/merchant/getValue")
    public ResponseEntity<String> getValue(
            @RequestParam(value = "merchantId") String merchantId,
            @RequestParam(value = "namespace") String namespace,
            @RequestParam(value = "key") String key
    ) {
        return ResponseEntity.ok(
                merchantService.getConfigurationValueByMerchantIdAndNamespaceAndKey(merchantId, namespace, key)
        );
    }

    @GetMapping("/merchant/getValueDto")
    public ResponseEntity<ConfigurationDto> getValueDto(
            @RequestParam(value = "merchantId") String merchantId,
            @RequestParam(value = "namespace") String namespace,
            @RequestParam(value = "key") String key
    ) {
        return ResponseEntity.ok(
                merchantService.getConfigurationByMerchantIdAndNamespaceAndKey(merchantId, namespace, key));
    }

    @GetMapping("/merchant/list-by-namespace")
    public ResponseEntity<List<MerchantDto>> getMerchantsByNamespace(@RequestParam(value = "namespace") String namespace) {
        List<MerchantDto> merchantDtos = merchantService.findAllByNamespace(namespace);

        return ResponseEntity.ok(
                merchantDtos
        );
    }

    @GetMapping("/merchant/get")
    public ResponseEntity<MerchantDto> getMerchantByMerchantId(@RequestParam(value = "merchantId") String merchantId,
                                                               @RequestParam(value="namespace") String namespace) {
        MerchantDto merchantDto = merchantService.findByMerchantIdAndNamespace(merchantId, namespace);
        return ResponseEntity.ok(merchantDto);
    }

    @GetMapping("/merchant/paytrail-secret/add")
    public ResponseEntity<MerchantDto> addPaytrailSecret(
            @RequestParam(value = "merchantId") String merchantId,
            @RequestParam(value = "secret") String secret
    ) {
        String salt = env.getRequiredProperty("merchant.secret.encryption.salt");

        String encryptedPaytrailSecret = encryptService.encryptSecret(secret, salt);

        MerchantDto merchantDto = merchantService.addMerchantConfiguration(
                merchantId,
                ServiceConfigurationKeys.MERCHANT_PAYTRAIL_SECRET,
                encryptedPaytrailSecret
        );
        return ResponseEntity.ok(merchantDto);
    }

    @GetMapping("/merchant/paytrail-secret/get")
    public ResponseEntity<String> getPaytrailSecret(@RequestParam(value = "merchantId") String merchantId) {
        String salt = env.getRequiredProperty("merchant.secret.encryption.salt");

        String encryptedPaytrailSecret = merchantService.getConfigurationValueByMerchantIdAndKey(
                merchantId,
                ServiceConfigurationKeys.MERCHANT_PAYTRAIL_SECRET
        );

        String paytrailSecret = decryptService.decryptSecret(salt, encryptedPaytrailSecret);

        return ResponseEntity.ok(paytrailSecret);
    }

    @PostMapping("/merchant/paytrail-merchant-mapping/add")
    public ResponseEntity<PaytrailMerchantMappingDto> addPaytrailMerchantMapping(@RequestBody PaytrailMerchantMappingDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                merchantService.save(dto)
        );
    }

    @GetMapping("/merchant/paytrail-merchant-mapping/get")
    public ResponseEntity<PaytrailMerchantMappingDto> getPaytrailMerchantMapping(
            @RequestParam(value = "merchantPaytrailMerchantId") String merchantPaytrailMerchantId,
            @RequestParam(value = "namespace") String namespace
    ) {
        return ResponseEntity.ok(
                merchantService.getPaytrailMerchantMappingByMerchantPaytrailMerchantIdAndNamespace(merchantPaytrailMerchantId, namespace)
        );
    }
}
