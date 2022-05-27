package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentMethodListRequest;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.service.OfflinePaymentService;
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
public class OfflinePaymentController {

    @Autowired
    private OfflinePaymentService offlinePaymentService;

    @PostMapping(value = "/payment/offline/get-available-methods", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentMethodDto[]> getAvailableMethods(@RequestBody GetPaymentMethodListRequest request) {
        try {
            String namespace = request.getNamespace();
            PaymentMethodDto[] filterPaymentMethodList = offlinePaymentService.getFilteredPaymentMethodList(request);

            if (filterPaymentMethodList == null) {
                log.debug("offline payment methods not found, namespace: " + namespace);
                throw new CommonApiException(
                        HttpStatus.NOT_FOUND,
                        new Error("offline-payment-methods-not-found-from-backend",
                                "offline payment methods for namespace[" + namespace + "] not found from backend")
                );
            }

            return ResponseEntity.status(HttpStatus.OK).body(filterPaymentMethodList);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting offline payment methods failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-offline-payment-methods", "failed to get offline payment methods")
            );
        }
    }


}
