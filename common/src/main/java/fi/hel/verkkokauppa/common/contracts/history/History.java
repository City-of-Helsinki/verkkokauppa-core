package fi.hel.verkkokauppa.common.contracts.history;

import java.time.LocalDateTime;

public interface History {
    static Boolean $default$isVisible() {
        return false;
    }

    String getEntityId();

    Boolean getIsVisible();

    String getEntityType();

    LocalDateTime getCreatedAt();

    String getNamespace();

    String getEventType();

    String getPayload();

    String getDescription();

    void setEntityId(String entityId);

    void setIsVisible(Boolean isVisible);

    void setEntityType(String entityType);

    void setCreatedAt(LocalDateTime createdAt);

    void setNamespace(String namespace);

    void setPayload(String payload);

    void setDescription(String description);
}
