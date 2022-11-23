package fi.hel.verkkokauppa.payment.paytrail.converter.impl;

import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.logic.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.converter.IPaytrailPayloadConverter;
import fi.hel.verkkokauppa.payment.util.PaymentUtil;
import org.helsinki.paytrail.model.payments.PaymentCallbackUrls;
import org.helsinki.paytrail.model.payments.PaymentCustomer;
import org.helsinki.paytrail.model.payments.PaymentItem;
import org.helsinki.paytrail.request.payments.PaytrailPaymentCreateRequest.CreatePaymentPayload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class PaytrailCreatePaymentPayloadConverter implements IPaytrailPayloadConverter<CreatePaymentPayload, OrderWrapper> {


    @Override
    public CreatePaymentPayload convertToPayload(PaytrailPaymentContext context, OrderWrapper orderWrapper) {
        OrderDto orderDto = orderWrapper.getOrder();

        CreatePaymentPayload payload = new CreatePaymentPayload();
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
        redirectUrls.setSuccess(context.getReturnUrl() + "/success");
        redirectUrls.setCancel(context.getReturnUrl() + "/cancel");

        PaymentCallbackUrls callbackUrls = new PaymentCallbackUrls();
        callbackUrls.setSuccess(context.getNotifyUrl() + "/success");
        callbackUrls.setCancel(context.getNotifyUrl() + "/cancel");

        payload.setRedirectUrls(redirectUrls);
        payload.setCallbackUrls(callbackUrls);

        return payload;
    }

    private ArrayList<PaymentItem> createPaytrailPaymentItemsFromOrderItems(PaytrailPaymentContext context, List<OrderItemDto> orderItemDtos) {
        ArrayList<PaymentItem> paymentItems = new ArrayList<>();

        for (OrderItemDto orderItemDto : orderItemDtos) {
            PaymentItem paymentItem = new PaymentItem();

            paymentItem.setStamp(orderItemDto.getOrderItemId());
            paymentItem.setReference(orderItemDto.getOrderItemId());
            paymentItem.setMerchant(context.getShopId());
            paymentItem.setUnitPrice(PaymentUtil.convertToCents(orderItemDto.getPriceGross()).intValue());
            paymentItem.setUnits(orderItemDto.getQuantity());
            paymentItem.setVatPercentage(Integer.valueOf(orderItemDto.getVatPercentage()));
            paymentItem.setProductCode(orderItemDto.getProductId());
            paymentItem.setDescription(orderItemDto.getProductName());
            paymentItem.setOrderId(orderItemDto.getOrderId());

            paymentItems.add(paymentItem);
        }

        return paymentItems;
    }

}