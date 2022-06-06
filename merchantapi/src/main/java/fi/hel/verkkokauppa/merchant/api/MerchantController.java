package fi.hel.verkkokauppa.merchant.api;

import fi.hel.verkkokauppa.merchant.service.MerchantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
public class MerchantController {
    @Autowired
    private MerchantService merchantService;


}
