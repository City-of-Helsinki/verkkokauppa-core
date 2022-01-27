package fi.hel.verkkokauppa.common.history;

import fi.hel.verkkokauppa.common.contracts.history.History;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HistoryDto implements History {
    String entityId;
    @Builder.Default
    Boolean isVisible = false;
    String entityType;
    LocalDateTime createdAt;
    String namespace;
    String eventType;
    String payload;
    String description;
}
