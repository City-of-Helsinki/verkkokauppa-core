package fi.hel.verkkokauppa.common.history.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.history.dto.HistoryDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HistoryUtil {
    @Autowired
    private ObjectMapper objectMapper;

    public String toString(HistoryDto dto) throws JsonProcessingException {
        return objectMapper.writeValueAsString(dto);
    }
}
