package fi.hel.verkkokauppa.order.service.voucher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.productmapping.dto.ProductMappingDto;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;

import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.voucher.OrderItemVoucher;
import fi.hel.verkkokauppa.order.model.voucher.ReservedVoucherCode;
import fi.hel.verkkokauppa.order.model.voucher.TokenChargeRequestDto;
import fi.hel.verkkokauppa.order.model.voucher.TokenChargeResponseDto;
import fi.hel.verkkokauppa.order.repository.jpa.voucher.OrderItemVoucherRepository;
import fi.hel.verkkokauppa.order.repository.jpa.voucher.ReservedVoucherCodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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
    private OrderItemVoucherRepository orderItemVoucherRepository;

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private SendNotificationService sendNotificationService;

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 10;
    private static final int MAX_ATTEMPTS = 20;
    private final SecureRandom random = new SecureRandom();

    private static final String DUMMY_TOKEN = "dummy_token";

    // check if any voucher logic applies to this payment
    public void voucherPaidCheck(String merchantId, String namespace, List<OrderItem> orderItems) throws JsonProcessingException {

        // check if barcode or QR code configuration exists for this merchant
        String barOrQRCodeType = commonServiceConfigurationClient.getMerchantConfigurationValue(merchantId, namespace, ServiceConfigurationKeys.MERCHANT_BAR_QR_CODE_TYPE);
        if( barOrQRCodeType != null ) {
            String tokenChargingUrl = commonServiceConfigurationClient.getMerchantConfigurationValue(merchantId, namespace, ServiceConfigurationKeys.TOKEN_CHARGING_URL);


            // do for each order item
            for (OrderItem orderItem : orderItems) {
                //
                // populate orderItemVoucher
                //
                OrderItemVoucher orderItemVoucher = new OrderItemVoucher();
                orderItemVoucher.setOrderItemVoucherId(UUID.randomUUID().toString());
                orderItemVoucher.setNamespace(namespace);
                orderItemVoucher.setMerchantId(merchantId);
                orderItemVoucher.setOrderId(orderItem.getOrderId());
                orderItemVoucher.setAmountLeft(orderItem.getRowPriceTotal().toString());
                orderItemVoucher.setQuantityLeft(orderItem.getQuantity());
                LocalDateTime now = LocalDateTime.now();
                orderItemVoucher.setCreatedAt(now);
                orderItemVoucher.setUpdatedAt(now);
                // generate token for bar/QR code in case external system does not do it or is not used
                // for now just using DUMMY_TOKEN
                orderItemVoucher.setTokenName(DUMMY_TOKEN);

                // check if external system needs to be updated on this
                if( tokenChargingUrl != null ) {
                    // call external system to get QR code and other details
                    orderItemVoucher = createTokenChargeCall(orderItemVoucher, orderItem, tokenChargingUrl);
                }

                // save orderItemVoucher
                if( orderItemVoucher != null ) {
                    orderItemVoucherRepository.save(orderItemVoucher);
                }

            }
        }


    }


    //
    // voucher charge request to external system
    //
    private OrderItemVoucher createTokenChargeCall(OrderItemVoucher orderItemVoucher, OrderItem orderItem, String tokenChargingUrl) throws JsonProcessingException {
        String namespace = orderItemVoucher.getNamespace();

        // Create token charging request
        TokenChargeRequestDto requestDto = new TokenChargeRequestDto();
        requestDto.setToken(orderItemVoucher.getTokenName());
        requestDto.setProductId(orderItem.getProductId());
        requestDto.setOrderId(orderItem.getOrderId());
        requestDto.setOrderItemId(orderItem.getOrderItemId());
        requestDto.setAmount(orderItem.getRowPriceTotal().toString());
        requestDto.setQuantity(orderItem.getQuantity());

        // try to inform external system that voucher was paid and get token
        try {
            // get namespace entity id
            log.info("Fetching product-mapping to get namespaceEntityId for productId: " + orderItem.getProductId());
            JSONObject response = restServiceClient.makeGetCall(serviceUrls.getProductMappingServiceUrl() + "/get?productId=" + orderItem.getProductId());
            ProductMappingDto dto = objectMapper.readValue(response.toString(), ProductMappingDto.class);
            requestDto.setNamespaceEntityId(dto.getNamespaceEntityId());


            TokenChargeResponseDto responseDto = makeTokenChargeCall( tokenChargingUrl, requestDto, namespace );

            // get QR code, id and QR code retrieval url
            orderItemVoucher.setTokenId(responseDto.getTokenId());
            orderItemVoucher.setTokenName(responseDto.getTokenName());
            orderItemVoucher.setTokenQRCodeUrl(responseDto.getTokenQRCodeUrl());


        } catch (Exception firstException) {
            // Log the error and try again
            log.error("Getting voucher/QR Code info failed for first time for order: {} orderItem:{}", orderItem.getOrderId(), orderItem.getOrderItemId(), firstException);

            try {
                TokenChargeResponseDto responseDto = makeTokenChargeCall( tokenChargingUrl, requestDto, namespace );

                // get QR code, id and QR code retrieval url
                orderItemVoucher.setTokenId(responseDto.getTokenId());
                orderItemVoucher.setTokenName(responseDto.getTokenName());
                orderItemVoucher.setTokenQRCodeUrl(responseDto.getTokenQRCodeUrl());

            } catch (Exception e) {
                // create error message with order id and orderItemId
                String errorMessage = "Getting voucher/QR Code info failed for order: " + orderItem.getOrderId() + " orderItem:" + orderItem.getOrderItemId();
                log.error(errorMessage, e);
                // Send Error email notification
                sendNotificationService.sendErrorNotification(
                        errorMessage,
                        e.getMessage(),
                        "Error - " + errorMessage
                );

                // TODO: throw specific error or handle some other way?
                // should not send order confirmation but otherwise process correctly
                throw new RuntimeException(e);
            }
        }


        return orderItemVoucher;
    }


    // Make call to external system and map response
    private TokenChargeResponseDto makeTokenChargeCall(String tokenChargingUrl, TokenChargeRequestDto requestDto, String namespace) throws JsonProcessingException {
        String body = objectMapper.writeValueAsString(requestDto);
        log.info("Token charging request body : {}", requestDto);
        // Token charge call
        ResponseEntity<JSONObject> response;

        //headers.append("Authorization", `Bearer ${accessToken}`);
        JSONObject tokenChargeResponse = restServiceClient.makeAuthBearerPostCall(tokenChargingUrl, body, namespace);
        TokenChargeResponseDto resultDto = objectMapper.readValue(tokenChargeResponse.toString(), TokenChargeResponseDto.class);

        return resultDto;
    }


    //
    // create token and set it to used ones
    //
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
