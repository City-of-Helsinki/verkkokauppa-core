package fi.hel.verkkokauppa.merchant.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.hel.verkkokauppa.common.merchant.dto.MerchantDto;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.merchant.model.MerchantModel;
import fi.hel.verkkokauppa.merchant.repository.merchantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class merchantService {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private merchantRepository merchantRepository;

    public MerchantModel savemerchant(MerchantDto dto) {
        dto.setmerchantId(UUIDGenerator.generateType4UUID().toString());
        dto.setCreatedAt(LocalDateTime.now());
        return merchantRepository.save(objectMapper.convertValue(dto, MerchantModel.class));
    }

}
