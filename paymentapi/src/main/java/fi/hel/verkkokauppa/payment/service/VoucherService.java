package fi.hel.verkkokauppa.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.productmapping.dto.ProductMappingDto;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.ReservedVoucherCode;
import fi.hel.verkkokauppa.payment.model.TokenChargeRequestDto;
import fi.hel.verkkokauppa.payment.model.voucher.OrderItemVoucher;
import fi.hel.verkkokauppa.payment.repository.ReservedVoucherCodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
@Slf4j
public class VoucherService {

    @Autowired
    private ServiceUrls serviceUrls;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommonServiceConfigurationClient commonServiceConfigurationClient;

    @Autowired
    private ReservedVoucherCodeRepository reservedVoucherCodeRepository;

    @Autowired
    private RestServiceClient restServiceClient;

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 10;
    private static final int MAX_ATTEMPTS = 20;
    private final SecureRandom random = new SecureRandom();

    private static final String DUMMY_TOKEN = "dummy_token";

    // check if any voucher logic applies to this payment
    public void voucherPaidCheck(String merchantId, OrderWrapper orderWrapper, Payment payment) throws JsonProcessingException {

        // check if barcode or QR code configuration exists for this merchant
        String barOrQRCodeType = commonServiceConfigurationClient.getMerchantConfigurationValue(merchantId, payment.getNamespace(), ServiceConfigurationKeys.MERCHANT_BAR_QR_CODE_TYPE);
        if( barOrQRCodeType != null ) {
            String tokenChargingUrl = commonServiceConfigurationClient.getMerchantConfigurationValue(merchantId, payment.getNamespace(), ServiceConfigurationKeys.TOKEN_CHARGING_URL);
            OrderDto orderDto = orderWrapper.getOrder();

            // do for each order item
            List<OrderItemDto> orderItems = orderWrapper.getItems();
            for (OrderItemDto orderItem : orderItems) {
                //
                // populate orderItemVoucher
                //
                OrderItemVoucher orderItemVoucher = new OrderItemVoucher();
                orderItemVoucher.setOrderItemVoucherId(UUID.randomUUID().toString());
                orderItemVoucher.setNamespace(orderDto.getNamespace());
                orderItemVoucher.setMerchantId(merchantId);
                orderItemVoucher.setOrderId(orderDto.getOrderId());
                orderItemVoucher.setAmountLeft(orderItem.getRowPriceTotal().toString());
                orderItemVoucher.setQuantityLeft(orderItem.getQuantity());
                LocalDateTime now = LocalDateTime.now();
                orderItemVoucher.setCreatedAt(now);
                orderItemVoucher.setUpdatedAt(now);
                // generate token for bar/QR code in case external system does not do it or is not used
                //
                orderItemVoucher.setTokenName(DUMMY_TOKEN);

                // check if external system needs to be updated on this
                if( tokenChargingUrl != null ) {
                    //
                    // Create token charging request
                    //
                    TokenChargeRequestDto requestDto = new TokenChargeRequestDto();
                    requestDto.setToken(orderItemVoucher.getTokenName());
                    requestDto.setProductId(orderItem.getProductId());
                    requestDto.setOrderId(orderItem.getOrderId());
                    requestDto.setOrderItemId(orderItem.getOrderItemId());
                    requestDto.setAmount(orderItem.getRowPriceTotal().toString());
                    requestDto.setQuantity(orderItem.getQuantity());
                    // get namespace entity id
                    log.info("Fetching product-mapping to get namespaceEntityId for productId: " + orderItem.getProductId());
                    JSONObject response = restServiceClient.makeGetCall(serviceUrls.getProductMappingServiceUrl() + "/get?productId=" + orderItem.getProductId());
                    ProductMappingDto dto = objectMapper.readValue(response.toString(), ProductMappingDto.class);
                    requestDto.setNamespaceEntityId(dto.getNamespaceEntityId());

                    // try to inform external system that voucher was paid and get token
                    try {
                        orderItemVoucher = tokenChargeCall(orderItemVoucher, requestDto, tokenChargingUrl);
                    } catch (Exception e) {
                        log.debug("payment-api received ORDER_CREATED event for orderId: " + message.getOrderId());
                        // Send Error email notification
                        sendNotificationService.sendErrorNotification(
                                "Renewal payment for order " + existingPayment.getOrderId() + " already exists",
                                "Renewal payment for order " + existingPayment.getOrderId() + " already exists, not creating new one. Payment id " + existingPayment.getPaymentId() + " status " + existingPayment.getStatus()
                        );

                        // TODO: throw specific error or handle some other way?
                        // should not send order confirmation but otherwise process correctly
                        throw new RuntimeException(e);
                    }

                    // save orderItemVoucher
                }

            }
        }


    }


    // voucher charge request to external system
    private OrderItemVoucher tokenChargeCall(OrderItemVoucher itemVoucher, TokenChargeRequestDto requestDto, String tokenChargingUrl) throws JsonProcessingException {



        return null;
    }

    // create token and set it to used ones
    private String getNewVoucherToken(){
        // create new codes until we have one that is not used
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String code = generateVoucherToken();

            if (!reservedVoucherCodeRepository.existsByCode(code)) {
                // save the code as used one
                ReservedVoucherCode reservedVoucherCode = new ReservedVoucherCode();
                reservedVoucherCode.setCode(code);
                reservedVoucherCode.setCreatedAt(LocalDateTime.now());
                reservedVoucherCodeRepository.save( reservedVoucherCode );
                return code;
            }
        }

        throw new IllegalStateException(
                "Could not generate a unique voucher code after " + MAX_ATTEMPTS + " attempts"
        );
    }

    // generates random token with configured values
    public String generateVoucherToken() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(
                    random.nextInt(CHARACTERS.length())
            ));
        }

        return code.toString();
    }
}
