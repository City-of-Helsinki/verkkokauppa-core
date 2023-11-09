package fi.hel.verkkokauppa.common.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ServiceConfigurationKeysTest {

    @Test
    public void getNamespaceAndMerchantConfigurationKeys() throws JsonProcessingException {
        List<String> keys = ServiceConfigurationKeys.getNamespaceAndMerchantConfigurationKeys();

        List<String> knownKeys = Arrays.asList(
                "merchantName",
                "merchantStreet",
                "merchantZip",
                "merchantCity",
                "merchantEmail",
                "merchantPaytrailMerchantId",
                "merchantPhone",
                "merchantUrl",
                "merchantShopId",
                "merchantTermsOfServiceUrl",
                "orderRightOfPurchaseIsActive",
                "orderRightOfPurchaseUrl",
                "subscriptionPriceUrl",
                "orderCancelRedirectUrl",
                "orderSuccessRedirectUrl",
                "merchantOrderWebhookUrl",
                "merchantPaymentWebhookUrl",
                "merchantSubscriptionWebhookUrl",
                "merchantRefundWebhookUrl",
                "refundSuccessRedirectUrl",
                "subscriptionResolveProductUrl"
        );
        List<String> keysShouldExist = keys.stream().sorted().collect(Collectors.toList());
        List<String> keysToCheck = knownKeys.stream().sorted().collect(Collectors.toList());

        ObjectMapper mapper = new ObjectMapper();
        log.info(mapper.writeValueAsString(keysShouldExist));
        Assert.assertEquals(
                keysShouldExist,
                keysToCheck
        );
    }

    @Test
    public void getAllConfigurationKeys() throws JsonProcessingException {
        List<String> keys = ServiceConfigurationKeys.getAllConfigurationKeys();

        List<String> knownKeys = Arrays.asList(
                "merchantCity",
                "merchantEmail",
                "merchantName",
                "merchantOrderWebhookUrl",
                "merchantPaymentWebhookUrl",
                "merchantPaytrailMerchantId",
                "merchantPhone",
                "merchantRefundWebhookUrl",
                "merchantShopId",
                "merchantStreet",
                "merchantSubscriptionWebhookUrl",
                "merchantTermsOfServiceUrl",
                "merchantUrl",
                "merchantZip",
                "namespaceApiAccessToken",
                "orderCancelRedirectUrl",
                "orderRightOfPurchaseIsActive",
                "orderRightOfPurchaseUrl",
                "orderSuccessRedirectUrl",
                "refundSuccessRedirectUrl",
                "payment_api_key",
                "payment_api_version",
                "payment_cp",
                "payment_currency",
                "payment_encryption_key",
                "payment_language",
                "payment_notification_url",
                "payment_register_card_token",
                "payment_return_url",
                "payment_submerchant_id",
                "payment_type",
                "subscriptionPriceUrl",
                "subscriptionResolveProductUrl"
        );
        List<String> keysShouldExist = keys.stream().sorted().collect(Collectors.toList());
        List<String> keysToCheck = knownKeys.stream().sorted().collect(Collectors.toList());

        ObjectMapper mapper = new ObjectMapper();
        log.info(mapper.writeValueAsString(keysShouldExist));

        Assert.assertEquals(
                keysShouldExist,
                keysToCheck
        );
    }

    @Test
    public void getOverridableMerchantKeys() throws JsonProcessingException {
        List<String> namespaceKeys = ServiceConfigurationKeys.getNamespaceKeys();
        List<String> merchantKeys = ServiceConfigurationKeys.getMerchantKeys();
        List<String> overridableMerchantKeys = ServiceConfigurationKeys.getOverridableMerchantKeys();

        List<String> knownKeys = Arrays.asList(
                "merchantTermsOfServiceUrl",
                "orderRightOfPurchaseIsActive",
                "orderRightOfPurchaseUrl",
                "subscriptionPriceUrl",
                "subscriptionResolveProductUrl"
        );

        ObjectMapper mapper = new ObjectMapper();
        log.info(mapper.writeValueAsString(namespaceKeys));
        log.info(mapper.writeValueAsString(merchantKeys));
        log.info(mapper.writeValueAsString(overridableMerchantKeys));

        overridableMerchantKeys.forEach(overridableKey -> {
            // Both should have the overridable key
            Assert.assertTrue(namespaceKeys.contains(overridableKey));
            Assert.assertTrue(merchantKeys.contains(overridableKey));
        });

        List<String> keysShouldExist = overridableMerchantKeys.stream().sorted().collect(Collectors.toList());
        List<String> keysToCheck = knownKeys.stream().sorted().collect(Collectors.toList());

        Assert.assertEquals(
                keysShouldExist,
                keysToCheck
        );

    }

    @Test
    public void getNamespaceKeys() throws JsonProcessingException {
        List<String> keys = ServiceConfigurationKeys.getNamespaceKeys();

        List<String> knownKeys = Arrays.asList(
                "merchantOrderWebhookUrl",
                "merchantPaymentWebhookUrl",
                "merchantRefundWebhookUrl",
                "merchantSubscriptionWebhookUrl",
                "merchantTermsOfServiceUrl",
                "orderCancelRedirectUrl",
                "orderRightOfPurchaseIsActive",
                "orderRightOfPurchaseUrl",
                "orderSuccessRedirectUrl",
                "subscriptionPriceUrl",
                "refundSuccessRedirectUrl",
                "subscriptionResolveProductUrl"
        );
        List<String> keysShouldExist = keys.stream().sorted().collect(Collectors.toList());
        List<String> keysToCheck = knownKeys.stream().sorted().collect(Collectors.toList());

        ObjectMapper mapper = new ObjectMapper();
        log.info(mapper.writeValueAsString(keysShouldExist));

        Assert.assertEquals(
                keysShouldExist,
                keysToCheck
        );
    }

    @Test
    public void getMerchantKeys() throws JsonProcessingException {
        List<String> keys = ServiceConfigurationKeys.getMerchantKeys();

        List<String> knownKeys = Arrays.asList(
                "merchantCity",
                "merchantEmail",
                "merchantName",
                "merchantPaytrailMerchantId",
                "merchantPhone",
                "merchantShopId",
                "merchantStreet",
                "merchantTermsOfServiceUrl",
                "merchantUrl",
                "merchantZip",
                "orderRightOfPurchaseIsActive",
                "orderRightOfPurchaseUrl",
                "subscriptionPriceUrl",
                "subscriptionResolveProductUrl"
        );
        List<String> keysShouldExist = keys.stream().sorted().collect(Collectors.toList());
        List<String> keysToCheck = knownKeys.stream().sorted().collect(Collectors.toList());

        ObjectMapper mapper = new ObjectMapper();
        log.info(mapper.writeValueAsString(keysShouldExist));

        Assert.assertEquals(
                keysShouldExist,
                keysToCheck
        );
    }

    @Test
    public void getPlatformKeys() throws JsonProcessingException {
        List<String> keys = ServiceConfigurationKeys.getPlatformKeys();

        List<String> knownKeys = Arrays.asList(
                "namespaceApiAccessToken",
                "payment_api_key",
                "payment_api_version",
                "payment_cp",
                "payment_currency",
                "payment_encryption_key",
                "payment_language",
                "payment_notification_url",
                "payment_register_card_token",
                "payment_return_url",
                "payment_submerchant_id",
                "payment_type"
        );
        List<String> keysShouldExist = keys.stream().sorted().collect(Collectors.toList());
        List<String> keysToCheck = knownKeys.stream().sorted().collect(Collectors.toList());

        ObjectMapper mapper = new ObjectMapper();
        log.info(mapper.writeValueAsString(keysShouldExist));

        Assert.assertEquals(
                keysShouldExist,
                keysToCheck
        );
    }

}