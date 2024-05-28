package fi.hel.verkkokauppa.payment.paytrail.converter.impl;

import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.converter.IPaytrailPayloadConverter;
import fi.hel.verkkokauppa.payment.util.PaymentUtil;
import org.helsinki.paytrail.model.payments.PaymentCallbackUrls;
import org.helsinki.paytrail.model.payments.PaymentCustomer;
import org.helsinki.paytrail.model.payments.PaymentItem;
import org.helsinki.paytrail.request.payments.PaytrailPaymentCreateRequest.CreatePaymentPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;

@Component
public class PaytrailCreatePaymentPayloadFromOrderMessage implements IPaytrailPayloadConverter<CreatePaymentPayload, OrderMessage> {

    @Autowired
    private Environment env;

    // if not set defaults to false
    @Value("${elasticsearch.service.local.environment:#{false}}")
    private Boolean isLocalEnvironment;

    @Override
    public CreatePaymentPayload convertToPayload(PaytrailPaymentContext context, OrderMessage message, String stamp) {
        CreatePaymentPayload payload = new CreatePaymentPayload();
        payload.setStamp(stamp);
        payload.setReference(message.getOrderId());
        payload.setAmount(PaymentUtil.convertToCents(new BigDecimal(message.getPriceTotal())).intValue());
        payload.setCurrency(context.getDefaultCurrency());
        payload.setLanguage(context.getDefaultLanguage());

        /* Create payment items */
        ArrayList<PaymentItem> paymentItems = new ArrayList<>();
        PaymentItem paymentItem = new PaymentItem();
        if (context.isUseShopInShop()) {
            // TODO: Stamp has to be like: <orderItemId> + _at_ + <timestamp>
            paymentItem.setStamp(message.getOrderItemId());
            paymentItem.setOrderId(message.getOrderId());
        }
        paymentItem.setReference(message.getOrderItemId());
        paymentItem.setMerchant(context.getShopId());
        paymentItem.setUnitPrice(PaymentUtil.convertToCents(new BigDecimal(message.getPriceGross())).intValue());
        paymentItem.setUnits(Integer.parseInt(message.getProductQuantity()));
        paymentItem.setVatPercentage(Integer.parseInt(message.getVatPercentage()));
        paymentItem.setProductCode(message.getProductId());
        paymentItem.setDescription(message.getProductName());
        paymentItems.add(paymentItem);
        payload.setItems(paymentItems);

        /* Create payment customer */
        PaymentCustomer customer = new PaymentCustomer();
        customer.setEmail(message.getCustomerEmail());
        customer.setFirstName(message.getCustomerFirstName());
        customer.setLastName(message.getCustomerLastName());
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

        return payload;
    }
}
