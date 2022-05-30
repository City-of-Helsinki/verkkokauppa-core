package fi.hel.verkkokauppa.merchant.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.merchant.dto.MerchantDto;
import fi.hel.verkkokauppa.merchant.model.MerchantModel;
import fi.hel.verkkokauppa.merchant.service.merchantService;
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
public class merchantController {
    @Autowired
    private merchantService merchantService;


    @PostMapping(value = "/merchant/create", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MerchantDto> createmerchant(@RequestBody @Valid MerchantDto merchant) {
        try {
            MerchantModel createdModel = merchantService.savemerchant(merchant);
            MerchantDto returningDto = merchantService.mapToDto(createdModel);
            return ResponseEntity.status(HttpStatus.CREATED).body(returningDto);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("Saving merchant failed", e);
            Error error = new Error("failed-to-save-merchant", "failed to save merchant");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }

}
