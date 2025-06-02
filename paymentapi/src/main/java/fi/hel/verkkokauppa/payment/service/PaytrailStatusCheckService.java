package fi.hel.verkkokauppa.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.SendEventService;
import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.productmapping.dto.ProductMappingDto;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.service.GenerateCsvService;
import fi.hel.verkkokauppa.common.service.dto.CheckPaymentDto;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.EncryptorUtil;
import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.payment.api.data.*;
import fi.hel.verkkokauppa.payment.constant.PaymentGatewayEnum;
import fi.hel.verkkokauppa.payment.mapper.PaytrailPaymentProviderListMapper;
import fi.hel.verkkokauppa.payment.model.Payer;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentItem;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.model.paytrail.payment.PaytrailPaymentProviderModel;
import fi.hel.verkkokauppa.payment.paytrail.PaytrailPaymentClient;
import fi.hel.verkkokauppa.payment.paytrail.PaytrailPaymentStatusClient;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContextBuilder;
import fi.hel.verkkokauppa.payment.repository.PayerRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentItemRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentRepository;
import fi.hel.verkkokauppa.payment.util.PaymentUtil;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.constants.CheckoutAlgorithm;
import org.helsinki.paytrail.constants.CheckoutMethod;
import org.helsinki.paytrail.model.paymentmethods.PaytrailPaymentMethod;
import org.helsinki.paytrail.model.payments.PaytrailPayment;
import org.helsinki.paytrail.model.payments.PaytrailPaymentMitChargeSuccessResponse;
import org.helsinki.paytrail.model.payments.PaytrailPaymentResponse;
import org.helsinki.paytrail.model.tokenization.PaytrailTokenResponse;
import org.helsinki.paytrail.service.PaytrailSignatureService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;


@Service
@Slf4j
public class PaytrailStatusCheckService {
    @Autowired
    private SendNotificationService sendNotificationService;

    @Autowired
    private GenerateCsvService generateCsvService;

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ServiceUrls serviceUrls;

    @Autowired
    private ObjectMapper objectMapper;

    public String sendPaytrailStatusCheckReport(List<CheckPaymentDto> updatedPayments, List<String> errors){
        String message = "";
        String cause = "";
        String header = "Paytrail status check";
        if(!updatedPayments.isEmpty()) {
            header += " - Updated payments: " + updatedPayments.size();
            message += "Updated " + updatedPayments.size() + " payments ";
            // TODO: create csv data common service
            cause += generateCsvService.generateCsvData(updatedPayments);
            log.info("Payment for order {} updated in paytrail status check. \n{}", cause);
        }

        if(!errors.isEmpty()) {
            header += " - Errors: " + errors.size();
            cause += "\n\nErrors:\n" + errors;
        }


        if(!message.isEmpty() || !cause.isEmpty()) {
            // send error notification email
            sendNotificationService.sendErrorNotification(
                    message,
                    cause,
                    header
            );
        }

        return cause;
    }

    public String getMerchantIdByProductId(String productId) throws JsonProcessingException {
        JSONObject productMappingResponse = restServiceClient.makeAdminGetCall(serviceUrls.getProductMappingServiceUrl() + "/get?productId=" + productId);
        ProductMappingDto productMapping = objectMapper.readValue(productMappingResponse.toString(), ProductMappingDto.class);

        return productMapping.getMerchantId();
    }
}
