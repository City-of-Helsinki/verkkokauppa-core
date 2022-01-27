package fi.hel.verkkokauppa.history.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.hel.verkkokauppa.common.history.HistoryDto;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.history.model.HistoryModel;
import fi.hel.verkkokauppa.history.repository.HistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class HistoryService {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private HistoryRepository historyRepository;

    public void saveHistory(HistoryDto dto) {
        dto.setEntityId(UUIDGenerator.generateType4UUID().toString());
        dto.setCreatedAt(LocalDateTime.now());
        historyRepository.save(objectMapper.convertValue(dto, HistoryModel.class));
    }
}
