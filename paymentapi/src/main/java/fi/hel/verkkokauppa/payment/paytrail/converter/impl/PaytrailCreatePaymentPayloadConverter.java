package fi.hel.verkkokauppa.payment.paytrail.converter.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.converter.IPaytrailPayloadConverter;
import fi.hel.verkkokauppa.payment.util.PaymentUtil;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.model.payments.PaymentCallbackUrls;
import org.helsinki.paytrail.model.payments.PaymentCustomer;
import org.helsinki.paytrail.model.payments.PaymentItem;
import org.helsinki.paytrail.request.payments.PaytrailPaymentCreateRequest.CreatePaymentPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class PaytrailCreatePaymentPayloadConverter implements IPaytrailPayloadConverter<CreatePaymentPayload, OrderWrapper> {

    @Autowired
    private Environment env;

    @Autowired
    private ObjectMapper mapper;

    // if not set defaults to false
    @Value("${elasticsearch.service.local.environment:#{false}}")
    private Boolean isLocalEnvironment;

    @Override
    public CreatePaymentPayload convertToPayload(PaytrailPaymentContext context, OrderWrapper orderWrapper, String stamp) {
        OrderDto orderDto = orderWrapper.getOrder();

        CreatePaymentPayload payload = new CreatePaymentPayload();
        payload.setStamp(stamp);
        payload.setReference(orderDto.getOrderId());
        payload.setAmount(PaymentUtil.convertToCents(new BigDecimal(orderDto.getPriceTotal())).intValue());
        payload.setCurrency(context.getDefaultCurrency());
        payload.setLanguage(context.getDefaultLanguage());

        /* Create payment items */
        ArrayList<PaymentItem> paymentItems = createPaytrailPaymentItemsFromOrderItems(context, orderWrapper.getItems());
        payload.setItems(paymentItems);

        /* Create payment customer */
        PaymentCustomer customer = new PaymentCustomer();
        customer.setEmail(orderDto.getCustomerEmail());
        customer.setFirstName(orderDto.getCustomerFirstName());
        customer.setLastName(orderDto.getCustomerLastName());
        payload.setCustomer(customer);

        /* Set redirect and callback URL:s */
        PaymentCallbackUrls redirectUrls = new PaymentCallbackUrls();
        redirectUrls.setSuccess(env.getRequiredProperty("paytrail_payment_return_success_url"));
        redirectUrls.setCancel(env.getRequiredProperty("paytrail_payment_return_cancel_url"));

        PaymentCallbackUrls callbackUrls = new PaymentCallbackUrls();
        callbackUrls.setSuccess(env.getRequiredProperty("paytrail_payment_notify_success_url"));
        callbackUrls.setCancel(env.getRequiredProperty("paytrail_payment_notify_cancel_url"));

        // Get from context if local environment
        if (this.isLocalEnvironment && !context.getNotifyUrl().isEmpty() && context.getNotifyUrl().contains("ngrok")) {
            callbackUrls.setSuccess(context.getNotifyUrl() + "/success");
            callbackUrls.setCancel(context.getNotifyUrl() + "/cancel");
        }

        payload.setRedirectUrls(redirectUrls);
        payload.setCallbackUrls(callbackUrls);

        try {
            log.info("convertToPayload request payload: {}", mapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.info(e.getMessage());
        }
        return payload;
    }

    private ArrayList<PaymentItem> createPaytrailPaymentItemsFromOrderItems(PaytrailPaymentContext context, List<OrderItemDto> orderItemDtos) {
        ArrayList<PaymentItem> paymentItems = new ArrayList<>();

        for (OrderItemDto orderItemDto : orderItemDtos) {
            PaymentItem paymentItem = new PaymentItem();

            if (context.isUseShopInShop()) {
                // TODO: Stamp has to be like: <orderItemId> + _at_ + <timestamp>
                paymentItem.setStamp(orderItemDto.getOrderItemId());
                paymentItem.setOrderId(orderItemDto.getOrderId());
            }
            paymentItem.setReference(orderItemDto.getOrderItemId());
            paymentItem.setMerchant(context.getShopId());
            paymentItem.setUnitPrice(PaymentUtil.convertToCents(orderItemDto.getPriceGross()).intValue());
            paymentItem.setUnits(orderItemDto.getQuantity());
            // KYV-1064 support one decimal (paytrail limit) in VAT
            DecimalFormat df = new DecimalFormat("#.0");
            String roundedStr = df.format(Double.parseDouble(orderItemDto.getVatPercentage()));
            // parseDouble does not support , as decimal point. convert possible ,'s to .'s
            paymentItem.setVatPercentage(Double.parseDouble(roundedStr.replace(',','.')));
//            paymentItem.setVatPercentage(Integer.valueOf(orderItemDto.getVatPercentage()));
            paymentItem.setProductCode(orderItemDto.getProductId());
            paymentItem.setDescription(orderItemDto.getProductName());

            paymentItems.add(paymentItem);
        }

        return paymentItems;
    }

}
