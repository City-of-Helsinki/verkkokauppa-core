package fi.hel.verkkokauppa.history.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.history.HistoryDto;
import fi.hel.verkkokauppa.history.service.HistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class HistoryController {
    @Autowired
    private HistoryService historyService;


    @PostMapping(value = "/history/post", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HistoryDto> sendOrderMessage(@RequestBody HistoryDto history) {
        try {
            historyService.saveHistory(history);
            return ResponseEntity.status(HttpStatus.CREATED).body(history);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("Saving history failed", e);
            Error error = new Error("failed-to-save-history", "failed to save history");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }

}
