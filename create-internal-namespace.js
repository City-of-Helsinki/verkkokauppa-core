const dotenv = require('dotenv') // Import dotenv
const axios = require('axios') // Import Axios


/**
 * Load environment variables from the specified dotenv file.
 * @param {string} [DOTENV_FILE='.env'] - The name of the dotenv file.
 */
dotenv.config({
  path: process.env.DOTENV_FILE || '.env'
})

/**
 * Create a namespace using Axios POST request.
 *
 * @param {string} namespace - The namespace to create.
 * @param {Array<Object>} configurations - An array of configuration objects.
 * @param {string} [configurations[].key] - The configuration key.
 * @param {string} [configurations[].value] - The configuration value.
 * @param {boolean} [configurations[].restricted] - Whether the configuration is restricted.
 * @returns {Promise<void>} - A promise that resolves when the namespace is created.
 */
async function createNamespace(namespace, configurations) {
  try {
    // Get URLs from environment variables
    const url = `${ process.env.MERCHANT_CONFIGURATION_BACKEND_URL }/namespace/create`

    // Prepare data
    const data = {
      namespace: namespace,
      configurations: configurations
    }

    // Axios POST request
    const response = await axios.post(url, data, {
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      }
    })

    // Log response data
    console.log('Namespace created:', response.data)
    return response.data
  } catch (error) {
    // Log any errors
    console.error('There was a problem with the request:', error.message)
  }
}

// Usage
const namespace = process.env.NAMESPACE_NAME
const configurations = [
  {
    key: 'merchantOrderWebhookUrl',
    value: 'merchantOrderWebhookUrl',
    restricted: false
  },
  {
    key: 'merchantPaymentWebhookUrl',
    value: 'merchantPaymentWebhookUrl',
    restricted: false
  },
  {
    key: 'merchantRefundWebhookUrl',
    value: 'merchantRefundWebhookUrl',
    restricted: false
  },
  {
    key: 'merchantSubscriptionWebhookUrl',
    value: 'merchantSubscriptionWebhookUrl',
    restricted: false
  },
  {
    key: 'merchantTermsOfServiceUrl',
    value: 'merchantTermsOfServiceUrl',
    restricted: false
  },
  {
    key: 'orderCancelRedirectUrl',
    value: 'orderCancelRedirectUrl',
    restricted: false
  },
  {
    key: 'orderPaymentFailedRedirectUrl',
    value: 'orderPaymentFailedRedirectUrl',
    restricted: false
  },
  {
    key: 'orderRightOfPurchaseIsActive',
    value: 'orderRightOfPurchaseIsActive',
    restricted: false
  },
  {
    key: 'orderRightOfPurchaseUrl',
    value: 'orderRightOfPurchaseUrl',
    restricted: false
  },
  {
    key: 'orderSuccessRedirectUrl',
    value: 'orderSuccessRedirectUrl',
    restricted: false
  },
  {
    key: 'refundSuccessRedirectUrl',
    value: 'refundSuccessRedirectUrl',
    restricted: false
  },
  {
    key: 'sendMerchantTermsOfService',
    value: 'sendMerchantTermsOfService',
    restricted: false
  },
  {
    key: 'subscriptionPriceUrl',
    value: 'subscriptionPriceUrl',
    restricted: false
  },
  {
    key: 'subscriptionResolveProductUrl',
    value: 'subscriptionResolveProductUrl',
    restricted: false
  },
];

/**
 * Fetch service configuration from the specified namespace.
 *
 * @param {string} namespace - The namespace for the service configuration.
 * @returns {Promise<Object|null>} - A promise that resolves with the service configuration or null if there's an error.
 */
async function getApiAccessToken(namespace) {
  const url = `${process.env.PRODUCT_MAPPING_BACKEND_URL}/serviceconfiguration/api-access/get?namespace=${namespace}`;

  try {
    const response = await axios.get(url, {
      headers: {
        'Accept': '*/*'
      }
    });

    return response.data;
  } catch (error) {
    console.error('Error fetching service configuration, trying to create:', error.message);
    return null; // Return null if there's an error
  }
}

/**
 * Create service configuration for the specified namespace.
 *
 * @param {string} namespace - The namespace for the service configuration.
 * @returns {Promise<Object>} - A promise that resolves with the created service configuration.
 */
async function createApiAccess(namespace) {
  const url = `${process.env.PRODUCT_MAPPING_BACKEND_URL}/serviceconfiguration/api-access/create?namespace=${namespace}`;

  try {
    const response = await axios.get(url, {
      headers: {
        'Accept': '*/*'
      }
    });

    return response.data;
  } catch (error) {
    console.error('Error creating service configuration:', error.message);
    throw error; // Throw the error if the create request fails
  }
}

/**
 * Create a product using Axios POST request.
 *
 * @param {string} namespace - The namespace for the product.
 * @param {string} namespaceEntityId - The namespaceEntityId for the product.
 * @param {string} merchantId - The merchantId for the product.
 * @returns {Promise<Object>} - A promise that resolves with the response data.
 */
async function createProduct(namespace, namespaceEntityId, merchantId) {
  const url = 'http://localhost:8081/v1/product/';

  // Prepare data
  const data = {
    namespace: namespace,
    namespaceEntityId: namespaceEntityId,
    merchantId: merchantId
  };

  try {
    const response = await axios.post(url, data, {
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      }
    });

    return response.data;
  } catch (error) {
    console.error('Error creating product:', error.message);
    throw error; // Throw the error if the POST request fails
  }
}

/**
 * Check if a merchant with the specified name exists.
 *
 * @param {string} merchantName - The name of the merchant to check.
 * @param namespace
 * @returns {Promise<{ exists: boolean, merchantId?: string }>} - A promise that resolves with a boolean indicating whether the merchant exists and the merchantId if found.
 *  */
async function checkMerchantExists(merchantName, namespace) {
  const url = `${ process.env.MERCHANT_API_URL }list/merchants/${ namespace }`;

  try {
    const response = await axios.get(url);
    const merchants = response.data;

    // Find the merchant with the same merchantName
    for (const [merchantId, merchant] of Object.entries(merchants)) {
      const merchantNameConfig = merchant.configurations.find(config => config.key === 'merchantName');
      if (merchantNameConfig && merchantNameConfig.value === merchantName) {
        return { exists: true, merchantId: merchant?.merchantId };
      }
    }

    return { exists: false };
  } catch (error) {
    console.error('Error checking merchant existence:', error.message);
    throw error; // Throw the error if the GET request fails
  }
}

/**
 * Create a merchant using Axios POST request.
 *
 * @param {Object} merchantData - The merchant data.
 * @param {string} merchantData.merchantName - The name of the merchant.
 * @param {string} merchantData.merchantStreet - The street of the merchant.
 * @param {string} merchantData.merchantZip - The ZIP code of the merchant.
 * @param {string} merchantData.merchantCity - The city of the merchant.
 * @param {string} merchantData.merchantEmail - The email of the merchant.
 * @param {string} merchantData.merchantPhone - The phone number of the merchant.
 * @param {string} merchantData.merchantUrl - The URL of the merchant.
 * @param {string} merchantData.merchantTermsOfServiceUrl - The terms of service URL of the merchant.
 * @param {string} merchantData.merchantBusinessId - The business ID of the merchant.
 * @param {string} merchantData.merchantShopId - The shop ID of the merchant.
 * @param {string} merchantData.merchantPaytrailMerchantId - The Paytrail merchant ID of the merchant.
 * @returns {Promise<Object>} - A promise that resolves with the response data.
 * @param {string} namespace
 */
async function upsertMerchant(merchantData, namespace) {
  const url = `${ process.env.MERCHANT_API_URL }create/merchant/${ namespace }`;

  try {
    try {
      const response = await axios.post(url, merchantData);
      return response.data;
    } catch (error) {
      console.error('Error creating merchant:', error.message);
      throw error; // Throw the error if the POST request fails
    }
  } catch (error) {
    console.error('Error checking merchant existence:', error.message);
    throw error; // Throw the error if the GET request fails
  }
}

(async () => {
  const createdNamespace = await createNamespace(namespace, configurations)
  console.log(createdNamespace)

  let apiKey = await getApiAccessToken(namespace);

  if (!apiKey) {
    try {
      apiKey = await createApiAccess(namespace);


    } catch (error) {
      console.error('There was a problem:', error.message);
      return;
    }
  }
  // Set the default api-key header globally for Axios
  axios.defaults.headers.common['api-key'] = apiKey;

  const merchantData = {
    merchantName: 'string',
    merchantStreet: 'string',
    merchantZip: 'string',
    merchantCity: 'string',
    merchantEmail: 'string',
    merchantPhone: 'string',
    merchantUrl: 'string',
    merchantTermsOfServiceUrl: 'string',
    merchantBusinessId: 'string',
    merchantShopId: 'string',
    merchantPaytrailMerchantId: 'string'
  };

  // Check if the merchant with the same merchantName exists
  const { exists, merchantId } = await checkMerchantExists(merchantData.merchantName,namespace);


  try {
    if (exists) {
      merchantData.merchantId = merchantId
    }
    const merchant = await upsertMerchant(merchantData, namespace);
    console.log('Merchant created/ updated:', merchant);
  } catch (error) {
    console.error('There was a problem:', error.message);
  }

  const namespaceEntityId = 'string';

  try {
    const product = await createProduct(namespace, namespaceEntityId, merchantId);
    console.log('Product created:', product);
  } catch (error) {
    console.error('There was a problem:', error.message);
  }



})()
