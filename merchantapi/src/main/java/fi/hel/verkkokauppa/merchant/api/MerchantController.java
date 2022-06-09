package fi.hel.verkkokauppa.merchant.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.merchant.api.dto.MerchantDto;
import fi.hel.verkkokauppa.merchant.service.MerchantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@Validated
@RestController
public class MerchantController {
    @Autowired
    private MerchantService merchantService;


    /**
     * > Upserts a merchant
     *
     * @param merchantDto The object that will be passed to the service layer.
     * @return ResponseEntity<MerchantDto>
     */
    @PostMapping(value = "/merchant/upsert", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MerchantDto> upsertMerchant(@RequestBody @Valid MerchantDto merchantDto) {
        try {

            // Checking if the merchant already exists. If it does, it will update the merchant.
            if (merchantDto.getMerchantId() != null) {
                // Returning a response with a status code of 201 (CREATED) and the body of the response is the
                // merchantDto.
                return ResponseEntity.status(HttpStatus.CREATED).body(
                        merchantService.update(merchantDto)
                );
            }

            // Returning a response with a status code of 201 (CREATED) and the body of the response is the merchantDto.
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    merchantService.save(merchantDto)
            );
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-upsert-merchant", "failed to upsert merchant, merchantId:" + merchantDto.getMerchantId())
            );
        }
    }
}
