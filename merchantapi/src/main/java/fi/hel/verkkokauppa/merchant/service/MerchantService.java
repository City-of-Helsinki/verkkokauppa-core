package fi.hel.verkkokauppa.merchant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.merchant.repository.MerchantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MerchantService {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MerchantRepository merchantRepository;


}
