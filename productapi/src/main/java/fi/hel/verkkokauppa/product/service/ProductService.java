package fi.hel.verkkokauppa.product.service;

import fi.hel.verkkokauppa.product.model.Product;
import fi.hel.verkkokauppa.product.repository.ProductRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ProductService {

    private Logger log = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MappingServiceClient mappingServiceClient;

    @Autowired
    private Environment env;


    public Product findById(String productId) {
        return getFromBackend(productId);
    }

    public Product save(Product product) {
        // You can add additional validation or business logic here
        return productRepository.save(product);
    }

    // TODO wip, returns on first call but not consecutive calls
    /*
    public Product getFromCache(String productId) {
        Optional<Product> cachedProduct = productRepository.findById(productId);
        
        if (cachedProduct.isPresent())
            return cachedProduct.get();

        log.debug("product not found from cache, productId: " + productId);
        return null;
    }
    */

    public Product getFromBackend(String productId) {
        try {
            // initialize http client
            WebClient client = mappingServiceClient.getClient();

            // query product mapping from common product mapping service
            JSONObject productMapping = mappingServiceClient.queryJsonService(client, env.getProperty("productmapping.url") + productId);

            String namespace = (String) productMapping.get("namespace");
            String namespaceEntityId = (String) productMapping.get("namespaceEntityId");
            log.debug("namespace: " + namespace + " namespaceEntityId: " + namespaceEntityId);

            // resolve original product backend from common service mapping service
            String serviceUrl = mappingServiceClient.resolveServiceUrl(client, namespace, "product");

            // query product data from origin backend service
            JSONObject originalProduct = mappingServiceClient.queryJsonService(client, serviceUrl + namespaceEntityId);
            String productName = (String) originalProduct.get("name");

            // construct a common product with mapping and original content
            Product product = new Product(productId, productName, productMapping, originalProduct);
            log.debug("product: " + product);

            // store to cache
            productRepository.save(product);

            return product;
        } catch (Exception e) {
            log.error("getting product from backend failed, productId: " + productId, e);
        }

        log.debug("product not found from backend, productId: " + productId);
        return null;
    }


    public Product createInternalProduct(String productId, String internalProductName) {
        try {
            // initialize http client
            WebClient client = mappingServiceClient.getClient();

            // query product mapping from common product mapping service
            JSONObject productMapping = mappingServiceClient.queryJsonService(client, env.getProperty("productmapping.url") + productId);

            String namespace = (String) productMapping.get("namespace");
            String namespaceEntityId = (String) productMapping.get("namespaceEntityId");
            log.debug("namespace: " + namespace + " namespaceEntityId: " + namespaceEntityId);

            // query product data from origin backend service
            JSONObject originalProduct = new JSONObject();
            originalProduct.put("name", internalProductName);

            String productName = (String) originalProduct.get("name");

            // construct a common product with mapping and original content
            Product product = new Product(productId, productName, productMapping, originalProduct);
            log.debug("internal product: " + product);

            // store to cache
            productRepository.save(product);

            return product;
        } catch (Exception e) {
            log.error("creating internal product backend failed, productId: " + productId, e);
        }

        log.debug("product not found from backend, productId: " + productId);
        return null;
    }

    public Product resolveProductFromInternalDatabase(String namespaceEntityId) {
        try {
            // initialize http client
            WebClient client = mappingServiceClient.getClient();

            // query product mapping from common product mapping service
            JSONObject productMapping = mappingServiceClient.queryJsonService(client, env.getProperty("productmapping.internal.url") + namespaceEntityId);

            String productId = (String) productMapping.get("productId");

            return this.productRepository.findById(productId).orElseThrow();
        } catch (Exception e) {
            log.error("Fetching internal product backend failed, namespaceEntityId: " + namespaceEntityId, e);
        }

        log.debug("product not found from backend, namespaceEntityId: " + namespaceEntityId);
        return null;
    }
}
