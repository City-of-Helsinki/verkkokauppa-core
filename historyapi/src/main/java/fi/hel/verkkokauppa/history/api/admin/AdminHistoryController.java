package fi.hel.verkkokauppa.history.api.admin;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.history.dto.HistoryDto;
import fi.hel.verkkokauppa.history.dto.HistoryWrapper;
import fi.hel.verkkokauppa.history.model.HistoryModel;
import fi.hel.verkkokauppa.history.service.HistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class AdminHistoryController {
    @Autowired
    private HistoryService historyService;

    @GetMapping(value = "/admin/history/list/get-entity-id-event-type", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HistoryWrapper> listHistoryWithNamespaceAndEntityIdAndEventType(
            @RequestParam(value = "namespace") String namespace,
            @RequestParam(value = "entityId") String entityId,
            @RequestParam(value = "eventType") String eventType

    ) {
        try {
            List<HistoryModel> models = historyService.findHistoryModelsByNamespaceAndEntityIdAndEventType(
                    namespace,
                    entityId,
                    eventType
            );
            List<HistoryDto> histories = models.stream().map(model -> historyService.mapToDto(model)).collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK).body(new HistoryWrapper(histories));
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting histories failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-list-histories-entitity-id-event-type", "failed to list histories with namespace " + namespace)
            );
        }
    }


    @GetMapping(value = "/admin/history/list/get-event-type", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HistoryWrapper> listHistoryWithNamespaceAndEventType(
            @RequestParam(value = "namespace") String namespace,
            @RequestParam(value = "eventType") String eventType
    ) {
        try {
            List<HistoryModel> models = historyService.findHistoryModelsByNamespaceAndEventType(
                    namespace,
                    eventType
            );
            List<HistoryDto> histories = models.stream().map(model -> historyService.mapToDto(model)).collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK).body(new HistoryWrapper(histories));
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting histories failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-list-histories-event-type", "failed to list histories with namespace " + namespace)
            );
        }
    }


    @GetMapping(value = "/admin/history/list/get-entity-id", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HistoryWrapper> listHistoryWithNamespaceAndEntityId(
            @RequestParam(value = "namespace") String namespace,
            @RequestParam(value = "entityId") String entityId
    ) {
        try {
            List<HistoryModel> models = historyService.findHistoryModelsByNamespaceAndEventType(
                    namespace,
                    entityId
            );
            List<HistoryDto> histories = models.stream().map(model -> historyService.mapToDto(model)).collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK).body(new HistoryWrapper(histories));
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting histories failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-list-histories-entity-id", "failed to list histories with namespace " + namespace)
            );
        }
    }
}
