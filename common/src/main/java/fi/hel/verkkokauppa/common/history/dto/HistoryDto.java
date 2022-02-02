package fi.hel.verkkokauppa.common.history.dto;

import fi.hel.verkkokauppa.common.contracts.history.History;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;

@Data
@Builder
public class HistoryDto implements History {
    String historyId;
    @NonNull
    String entityId;
    String user;

    @Builder.Default
    Boolean isVisible = false;
    @NonNull
    String entityType;
    LocalDateTime createdAt;
    @NonNull
    String namespace;
    @NonNull
    String eventType;
    String payload;
    String description;
}
