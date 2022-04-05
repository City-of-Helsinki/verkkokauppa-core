package fi.hel.verkkokauppa.history.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.hel.verkkokauppa.common.history.dto.HistoryDto;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.history.model.HistoryModel;
import fi.hel.verkkokauppa.history.repository.HistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class HistoryService {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private HistoryRepository historyRepository;

    public HistoryModel saveHistory(HistoryDto dto) {
        dto.setHistoryId(UUIDGenerator.generateType4UUID().toString());
        dto.setCreatedAt(LocalDateTime.now());
        return historyRepository.save(objectMapper.convertValue(dto, HistoryModel.class));
    }

    public HistoryDto mapToDto(HistoryModel model) {
        return objectMapper.convertValue(model, HistoryDto.class);
    }

    public List<HistoryModel> findHistoryModelsByNamespaceAndEntityIdAndEventType(String namespace, String entityId, String eventType) {
        return historyRepository.findHistoryModelsByNamespaceAndEntityIdAndEventType(
                namespace,
                entityId,
                eventType
        );
    }


    public List<HistoryModel> findHistoryModelsByNamespaceAndEventType(String namespace, String eventType) {
        return historyRepository.findHistoryModelsByNamespaceAndEventType(
                namespace,
                eventType
        );
    }


    public List<HistoryModel> findHistoryModelsByNamespaceAndEntityId(String namespace, String entityId) {
        return historyRepository.findHistoryModelsByNamespaceAndEventType(
                namespace,
                entityId
        );
    }

}
