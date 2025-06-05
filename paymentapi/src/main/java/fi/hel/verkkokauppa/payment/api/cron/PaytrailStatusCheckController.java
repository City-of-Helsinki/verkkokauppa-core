package fi.hel.verkkokauppa.payment.api.cron;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.common.productmapping.dto.ProductMappingDto;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.service.GenerateCsvService;
import fi.hel.verkkokauppa.common.service.dto.CheckPaymentDto;
import fi.hel.verkkokauppa.payment.api.cron.dto.SynchronizeResultDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentItem;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.paytrail.PaytrailPaymentStatusClient;
import fi.hel.verkkokauppa.payment.paytrail.validation.PaytrailPaymentReturnValidator;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.service.PaymentPaytrailService;
import fi.hel.verkkokauppa.payment.service.PaytrailStatusCheckService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.model.payments.PaytrailPayment;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@Slf4j
public class PaytrailStatusCheckController {

    @Autowired
    private OnlinePaymentService onlinePaymentService;

    @Autowired
    PaymentPaytrailService paymentPaytrailService;

    @Autowired
    private PaytrailPaymentReturnValidator paytrailPaymentReturnValidator;

    @Autowired
    private SaveHistoryService saveHistoryService;

    @Autowired
    private SendNotificationService sendNotificationService;

    @Autowired
    private CommonServiceConfigurationClient commonServiceConfigurationClient;

    @Autowired
    private PaytrailStatusCheckService paytrailStatusCheckService;


    @GetMapping(value = "/synchronize-paytrail-payment-status", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> checkAndSynchronizePaytrailPaymentStatus(
            @Parameter( description = "Datetime after which to check the payments. Defaults to one day ago. Example: 2025-05-10T00:00:00" )
            @RequestParam(value = "createdAfter", required = false) String createdAfter,
            @Parameter( description = "Datetime before which to check the payments. Defaults to now - 5 minutes. Example 2025-05-12T12:00:00" )
            @RequestParam(value = "createdBefore", required = false) String createdBefore
    ) {
        List<String> errors = new ArrayList();
        List<CheckPaymentDto> updatedPayments = new ArrayList();

        LocalDateTime createdAfterDateTime = null;
        LocalDateTime createdBeforeDateTime = null;
        // Define date-time formatter
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        // Parse the parameters if they are provided
        if ( createdAfter != null ) {
            createdAfterDateTime = LocalDateTime.parse(createdAfter, formatter);
        } else {
            // startdatetime not provided, check for today
            createdAfterDateTime = LocalDateTime.now().minusDays(1);
        }

        if ( createdBefore != null ) {
            createdBeforeDateTime = LocalDateTime.parse(createdBefore, formatter);
        } else {
            // enddatetime not provided, check for today
            createdBeforeDateTime = LocalDateTime.now().minusMinutes(5);
        }
        log.info("Synchronizing payments between {} and {}", createdAfterDateTime, createdBeforeDateTime);

        try {
            // get list of payments to synchronize
            List<CheckPaymentDto> payments = onlinePaymentService.getUnpaidPaymentsToCheck(createdAfterDateTime, createdBeforeDateTime);

            // get paytrail statuses for payments
            for( CheckPaymentDto payment : payments ) {
                try {
                    if( payment == null ){
                        continue;
                    }

                    // get product id from first paymentItem
                    List<PaymentItem> items = onlinePaymentService.getPaymentItemsForPayment(payment.getPaymentId());

                    if (items.size() <= 0) {
                        String error = "Payment " + payment.getPaymentId() + " had no items. Not checking status from paytrail";
                        log.error(error);
                        // add error to list
                        errors.add(error);
                        continue;
                    }

                    log.info("Synchronizing payments between {} and {}", createdAfterDateTime, createdBeforeDateTime);
//                    String productId = items.get(0).getProductId();
                    String productId = items.stream()
                            .filter(Objects::nonNull)
                            .map(PaymentItem::getProductId)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null);

                    // get merchant id for payment from product mapping
                    String merchantId = paytrailStatusCheckService.getMerchantIdByProductId(productId);

                    // get paytrail payment status
                    PaytrailPayment paytrailPayment = paymentPaytrailService.getPaytrailPayment(payment.getPaytrailTransactionId(), payment.getNamespace(), merchantId);

                    if (payment.getPaymentProviderStatus() != null && payment.getPaymentProviderStatus().equals(paytrailPayment.status)){
                        log.info("Payment provider status has not changed for payment {}, skipping paytrail status check for this", payment.getPaymentId());
                        continue;
                    }

                    log.info("Payment provider status has changed for payment {}. New status: {}", payment.getPaymentId(), paytrailPayment.getStatus());

                    PaymentReturnDto paymentReturnDto = paytrailPaymentReturnValidator.validateReturnValues(true, paytrailPayment.status, null);
                    if (payment.getStatus() != null && payment.getStatus().equals(PaymentStatus.CREATED_FOR_MIT_CHARGE)) {

                        // for mit charge we do not do retries
                        paymentReturnDto.setCanRetry(false);
                    }
                    onlinePaymentService.updatePaymentStatus(payment.getPaymentId(), paymentReturnDto);
                    Payment updatedPayment = paymentPaytrailService.updatePaymentWithPaytrailPayment(payment.getPaymentId(), paytrailPayment);

                    String updateText = "New status: " + updatedPayment.getStatus() + ". New payment provider status: " + updatedPayment.getPaymentProviderStatus();

                    // log new values to history table
                    saveHistoryService.saveCustomPaymentMessageHistory(PaymentMessage.builder()
                                    .orderId(payment.getOrderId())
                                    .paymentId(payment.getPaymentId())
                                    .namespace(payment.getNamespace())
                                    .eventType("PAYMENT_STATUS_CHECK_UPDATE")
                                    .eventTimestamp(LocalDateTime.now().toString())
                                    .paymentPaidTimestamp(payment.getTimestamp())
                                    .build(),
                            "Payment updated in paytrail status check. " + updateText
                    );
                    // add paytrail merchant id
                    String paytrailMerchantId = commonServiceConfigurationClient.getMerchantConfigurationValue(merchantId, payment.getNamespace(), ServiceConfigurationKeys.MERCHANT_PAYTRAIL_MERCHANT_ID);
                    payment.setPaytrailMerchantId(paytrailMerchantId);

                    // add to email (include paytrail merchant id)
                    payment.setPaidAt(updatedPayment.getPaidAt());
                    payment.setStatus(updatedPayment.getStatus());
                    payment.setPaymentProviderStatus(updatedPayment.getPaymentProviderStatus());
                    updatedPayments.add(payment);
                    log.info("Payment for order {} updated in paytrail status check. {}", payment.getOrderId(), updateText);

                } catch (Exception e) {
                    String error = "PaytrailPaymentStatusCheck - error processing payment " + payment.getPaymentId();
                    log.error("{}\n{}", error, e);
                    errors.add(error + " " + e.getMessage());
                }
            }

            String response = paytrailStatusCheckService.sendPaytrailStatusCheckReport(updatedPayments, errors);
            return ResponseEntity.ok().body(response);

        } catch (CommonApiException cae) {
            log.error("Failed to synchronize paytrail status", cae);
            sendNotificationService.sendErrorNotification(
                    cae.getMessage(),
                    cae.toString(),
                    "Failed to synchronize paytrail status"
            );
            throw cae;
        } catch (Exception e) {
            log.error("Failed to synchronize paytrail status", e);
            sendNotificationService.sendErrorNotification(
                    e.getMessage(),
                    e.toString(),
                    "Failed to synchronize paytrail status"
            );
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-synchronize-paytrail-status", "Failed to synchronize paytrail status")
            );
        }
    }

}