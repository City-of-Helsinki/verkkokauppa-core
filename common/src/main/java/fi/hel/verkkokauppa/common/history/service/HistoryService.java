package fi.hel.verkkokauppa.common.history.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.message.EventMessage;
import fi.hel.verkkokauppa.common.history.model.HistoryModel;
import fi.hel.verkkokauppa.common.history.repository.HistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HistoryService {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private HistoryRepository historyRepository;

    public void saveObjectMessage(String entityType,
                                  String namespace,
                                  String eventType,
                                  Boolean isVisible,
                                  Object payload,
                                  Object message ) {

    }
}
