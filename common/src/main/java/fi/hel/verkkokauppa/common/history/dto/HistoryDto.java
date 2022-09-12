package fi.hel.verkkokauppa.common.history.dto;

import fi.hel.verkkokauppa.common.contracts.history.History;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@Builder
public class HistoryDto implements History {
    String historyId;
    @NotBlank(message = "entityId cant be blank")
    String entityId;
    String user;

    @Builder.Default
    Boolean isVisible = false;

    @NotBlank(message = "entityType cant be blank")
    String entityType;

    LocalDateTime createdAt;

    @NotBlank(message = "namespace cant be blank")
    String namespace;

    @NotBlank(message = "eventType cant be blank")
    String eventType;

    String payload;
    String description;
}
