package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.common.rest.dto.ErrorNotificationDto;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class OrderErrorNotificationController {

    @Autowired
    private SendNotificationService sendNotificationService;

    @Autowired
    private OrderService orderService;


    @PostMapping(value = "/notification/sendErrorNotificationWithOrder/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> sendErrorNotificationWithOrderInfo(@PathVariable String orderId, @RequestBody ErrorNotificationDto dto) {
        try {
            // Get extra text message from service for detailed order information
            String extraText = orderService.collectDetailedInformationForNotification(orderId);

            sendNotificationService.sendErrorNotification(dto.getMessage() + extraText, dto.getCause(), dto.getHeader());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("Sending error notification failed {}", dto);
            Error error = new Error("failed-to-send-error-notification", "failed to send error notification");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }

    }
}