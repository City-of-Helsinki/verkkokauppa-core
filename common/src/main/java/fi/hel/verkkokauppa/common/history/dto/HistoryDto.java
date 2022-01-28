package fi.hel.verkkokauppa.common.history.dto;

import fi.hel.verkkokauppa.common.contracts.history.History;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HistoryDto implements History {
    String historyId;
    String entityId;
    String user;

    @Builder.Default
    Boolean isVisible = false;

    String entityType;
    LocalDateTime createdAt;
    String namespace;
    String eventType;
    String payload;
    String description;
}
