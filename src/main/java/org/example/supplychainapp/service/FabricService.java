package org.example.supplychainapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@SuppressWarnings({"squid:S1166","squid:S2142"})
public class FabricService {
    private final Contract contract;
    private static final Logger logger = LoggerFactory.getLogger(FabricService.class);

    // --- constants to avoid duplicated literals ---
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_PRODUCT = "product";
    private static final String KEY_PRODUCT_ID = "productId";
    private static final String KEY_QUANTITY = "quantity";
    // Make this configurable via property so it's not a hard-coded URI
    @Value("${supplychain.remote.createProductPath:/fabric/assets/createProduct}")
    private String remoteCreateProductPath;

    private final boolean remoteEnabled;
    private final String remoteUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FabricService(Contract contract,
                         @Value("${supplychain.remote.enabled:false}") boolean remoteEnabled,
                         @Value("${supplychain.remote.url:}") String remoteUrl,
                         RestTemplate restTemplate) {
        this.contract = contract;
        this.remoteEnabled = remoteEnabled;
        this.remoteUrl = remoteUrl != null ? remoteUrl.replaceAll("/+$", "") : "";
        this.restTemplate = restTemplate;
    }

    // Helper to serialize payloads to JSON and wrap checked exceptions
    private String toJson(Object obj) throws FabricServiceException {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new FabricServiceException("Failed to serialize payload: " + e.getMessage(), e);
        }
    }

    // Shared helper to extract the "message" or fallback body from a remote ResponseEntity
    private String extractMessageFromBody(ResponseEntity<String> resp) throws FabricServiceException {
        try {
            Map<?, ?> map = objectMapper.readValue(resp.getBody(), Map.class);
            if (map != null && map.containsKey(KEY_MESSAGE) && map.get(KEY_MESSAGE) != null) {
                return map.get(KEY_MESSAGE).toString();
            }
            return resp.getBody() == null ? "" : resp.getBody();
        } catch (JsonProcessingException e) {
            String body = resp.getBody();
            throw new FabricServiceException("Failed to parse remote response body: " + (body == null ? "" : body), e);
        }
    }

    // Extracted remote audit-log fetch to reduce complexity in the main method
    private byte[] fetchAuditLogRemote(String productId) throws ContractException {
        String url = String.format("%s/fabric/assets/queryLogByProductId/%s", remoteUrl, productId);
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            Map<?, ?> map = objectMapper.readValue(resp.getBody(), Map.class);
            Object product = null;
            if (map != null && map.containsKey(KEY_PRODUCT)) {
                product = map.get(KEY_PRODUCT);
            } else if (resp.getBody() != null) {
                product = resp.getBody();
            }
            if (product == null) {
                throw new ContractException("Log not found");
            }
            return product.toString().getBytes();
        } catch (HttpStatusCodeException he) {
            String body = he.getResponseBodyAsString();
            throw new ContractException("Log not found for productId=" + productId + ", remoteBody=" + body, he);
        } catch (JsonProcessingException e) {
            throw new ContractException("Failed to parse remote response for productId=" + productId + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ContractException(String.format("Failed to read log with product id: %s", productId), e);
        }
    }

    public byte[] createProduct(String productId, String name, String category, String quantity) throws FabricServiceException {
        logger.info("Service: Creating product with id={}, name={}, category={}, quantity={}", productId, name, category, quantity);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            String url = remoteUrl + remoteCreateProductPath;
            Map<String, Object> payload = new HashMap<>();
            payload.put(KEY_PRODUCT_ID, productId);
            payload.put("productName", name);
            payload.put("category", category);
            payload.put(KEY_QUANTITY, Integer.parseInt(quantity));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(toJson(payload), headers);
            try {
                ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
                logger.info("Service: Remote product create returned status={}", resp.getStatusCode());
                String message = extractMessageFromBody(resp);
                if (message != null && message.toLowerCase().contains("already exists")) {
                    throw new ProductAlreadyExistsException(String.format("Product with id: %s already exists", productId));
                }
                return message == null ? new byte[0] : message.getBytes();
            } catch (HttpStatusCodeException he) {
                String body = he.getResponseBodyAsString();
                throw new FabricServiceException("Failed to create product: id=" + productId + ", remoteBody=" + body, he);
            } catch (Exception e) {
                throw new FabricServiceException("Failed to create product: id=" + productId + ": " + e.getMessage(), e);
            }
        }

        try {
            byte[] result = contract.submitTransaction("AssetContract:createProduct", productId, name, category, quantity);
            logger.info("Service: Product created successfully: id={}", productId);
            return result;
        } catch (Exception e) {
            throw new FabricServiceException("Failed to create product: id=" + productId + ": " + e.getMessage(), e);
        }
    }

    public byte[] readProduct(String productId) throws FabricServiceException {
        logger.info("Service: Reading product with id={}", productId);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            String url = String.format("%s/fabric/assets/queryProduct/%s", remoteUrl, productId);
            try {
                ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
                Map<?, ?> map = objectMapper.readValue(resp.getBody(), Map.class);
                Object product = null;
                if (map != null && map.containsKey(KEY_PRODUCT)) {
                    product = map.get(KEY_PRODUCT);
                } else if (resp.getBody() != null) {
                    product = resp.getBody();
                }
                if (product == null) {
                    throw new ProductNotFoundException(String.format("Product with id: %s is not found", productId));
                }
                return product.toString().getBytes();
            } catch (HttpStatusCodeException he) {
                String body = he.getResponseBodyAsString();
                throw new ProductNotFoundException(String.format("Product with id: %s is not found, remoteBody=%s", productId, body), he);
            } catch (Exception e) {
                throw new FabricServiceException("Failed to read product: id=" + productId + ": " + e.getMessage(), e);
            }
        }

        try {
            byte[] result = contract.evaluateTransaction("AssetContract:readProduct", productId);
            logger.info("Service: Product details fetched: id={}", productId);
            return result;
        } catch (Exception e) {
            throw new FabricServiceException("Failed to read product: id=" + productId + ": " + e.getMessage(), e);
        }
    }

    public byte[] updateProductQuantity(String productId, String quantity) throws FabricServiceException {
        logger.info("Service: Updating product quantity: id={}, new quantity={}", productId, quantity);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            String url = String.format("%s/fabric/assets/update/%s", remoteUrl, productId);
            Map<String, String> payload = new HashMap<>();
            payload.put(KEY_QUANTITY, quantity);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(toJson(payload), headers);
            try {
                ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
                String message = extractMessageFromBody(resp);
                return message == null ? new byte[0] : message.getBytes();
            } catch (HttpStatusCodeException he) {
                String body = he.getResponseBodyAsString();
                throw new FabricServiceException("Failed to update product: id=" + productId + ", remoteBody=" + body, he);
            } catch (Exception e) {
                throw new FabricServiceException("Failed to update product: id=" + productId + ": " + e.getMessage(), e);
            }
        }

        try {
            byte[] result = contract.submitTransaction("AssetContract:updateProductQuantity", productId, quantity);
            logger.info("Service: Product updated successfully: id={}", productId);
            return result;
        } catch (Exception e) {
            throw new FabricServiceException("Failed to update product: id=" + productId + ": " + e.getMessage(), e);
        }
    }

    public byte[] deleteProduct(String productId) throws FabricServiceException {
        logger.info("Service: Deleting product with id={}", productId);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            String url = String.format("%s/fabric/assets/removeProduct/%s", remoteUrl, productId);
            try {
                ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.DELETE, null, String.class);
                String message = extractMessageFromBody(resp);
                return message == null ? new byte[0] : message.getBytes();
            } catch (HttpStatusCodeException he) {
                String body = he.getResponseBodyAsString();
                throw new FabricServiceException("Failed to delete product: id=" + productId + ", remoteBody=" + body, he);
            } catch (Exception e) {
                throw new FabricServiceException("Failed to delete product: id=" + productId + ": " + e.getMessage(), e);
            }
        }

        try {
            byte[] result = contract.submitTransaction("AssetContract:deleteProduct", productId);
            logger.info("Service: Product deleted successfully: id={}", productId);
            return result;
        } catch (Exception e) {
            throw new FabricServiceException("Failed to delete product: id=" + productId + ": " + e.getMessage(), e);
        }
    }

    public byte[] createShipment(String shipmentId, String productId, String origin, String destination, String carrier,
                                 String quantity) throws FabricServiceException {
        logger.info("Service: Creating shipment: id={}, productId={}, origin={}, destination={}, carrier={}, quantity{}",
                shipmentId, productId, origin, destination, carrier, quantity);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            String url = remoteUrl + "/fabric/assets/createShipment";
            Map<String, Object> payload = new HashMap<>();
            payload.put("shipmentId", shipmentId);
            payload.put(KEY_PRODUCT_ID, productId);
            payload.put("origin", origin);
            payload.put("destination", destination);
            payload.put("carrier", carrier);
            payload.put(KEY_QUANTITY, Integer.parseInt(quantity));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(toJson(payload), headers);
            try {
                ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
                String message = extractMessageFromBody(resp);
                return message == null ? new byte[0] : message.getBytes();
            } catch (HttpStatusCodeException he) {
                String body = he.getResponseBodyAsString();
                throw new FabricServiceException("Failed to create shipment: id=" + shipmentId + ", remoteBody=" + body, he);
            } catch (Exception e) {
                throw new FabricServiceException("Failed to create shipment: id=" + shipmentId + ": " + e.getMessage(), e);
            }
        }

        try {
            byte[] result = contract.submitTransaction("ShipmentContract:createShipment", shipmentId, productId, origin, destination,
                    carrier, quantity);
            logger.info("Service: Shipment created successfully: id={}", shipmentId);
            return result;
        } catch (Exception e) {
            throw new FabricServiceException("Failed to create shipment: id=" + shipmentId + ": " + e.getMessage(), e);
        }
    }

    public byte[] getShipment(String shipmentId) throws FabricServiceException {
        logger.info("Service: Reading shipment with id={}", shipmentId);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            String url = String.format("%s/fabric/assets/queryShipment/%s", remoteUrl, shipmentId);
            try {
                ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
                Map<?, ?> map = objectMapper.readValue(resp.getBody(), Map.class);
                Object shipment = null;
                if (map != null && map.containsKey("shipment")) {
                    shipment = map.get("shipment");
                } else if (resp.getBody() != null) {
                    shipment = resp.getBody();
                }
                if (shipment == null) {
                    throw new FabricServiceException("Shipment not found");
                }
                return shipment.toString().getBytes();
            } catch (HttpStatusCodeException he) {
                String body = he.getResponseBodyAsString();
                throw new FabricServiceException("Shipment not found: id=" + shipmentId + ", remoteBody=" + body, he);
            } catch (Exception e) {
                throw new FabricServiceException("Failed to get shipment: id=" + shipmentId + ": " + e.getMessage(), e);
            }
        }

        try {
            byte[] result = contract.evaluateTransaction("ShipmentContract:getShipment", shipmentId);
            logger.info("Service: Shipment details fetched: id={}", shipmentId);
            return result;
        } catch (Exception e) {
            throw new FabricServiceException("Failed to read shipment: id=" + shipmentId + ": " + e.getMessage(), e);
        }
    }

    public byte[] updateShipmentStatus(String shipmentId, String status) throws FabricServiceException {
        logger.info("Service: Updating shipment status: id={}, new status={}", shipmentId, status);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            String url = String.format("%s/fabric/assets/updateShipment/%s", remoteUrl, shipmentId);
            Map<String, String> payload = new HashMap<>();
            payload.put("status", status);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(toJson(payload), headers);
            try {
                ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
                String message = extractMessageFromBody(resp);
                return message == null ? new byte[0] : message.getBytes();
            } catch (HttpStatusCodeException he) {
                String body = he.getResponseBodyAsString();
                throw new FabricServiceException("Failed to update shipment: id=" + shipmentId + ", remoteBody=" + body, he);
            } catch (Exception e) {
                throw new FabricServiceException("Failed to update shipment: id=" + shipmentId + ": " + e.getMessage(), e);
            }
        }

        try {
            byte[] result = contract.submitTransaction("ShipmentContract:updateShipmentStatus", shipmentId, status);
            logger.info("Service: Shipment updated successfully: id={}", shipmentId);
            return result;
        } catch (Exception e) {
            throw new FabricServiceException("Failed to update shipment: id=" + shipmentId + ": " + e.getMessage(), e);
        }
    }

    // New method to place an order (calls chaincode 'placeOrder')
    public byte[] placeOrder(String productId, String quantity) throws FabricServiceException {
        logger.info("Service: Placing order for productId={}, quantity={}", productId, quantity);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            String url = remoteUrl + "/fabric/assets/placeOrder";
            Map<String, Object> payload = new HashMap<>();
            payload.put(KEY_PRODUCT_ID, productId);
            payload.put(KEY_QUANTITY, Integer.parseInt(quantity));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(toJson(payload), headers);
            try {
                ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
                String message = extractMessageFromBody(resp);
                return message == null ? new byte[0] : message.getBytes();
            } catch (HttpStatusCodeException he) {
                String body = he.getResponseBodyAsString();
                throw new FabricServiceException("Failed to place order: productId=" + productId + ", remoteBody=" + body, he);
            } catch (Exception e) {
                throw new FabricServiceException("Failed to place order: productId=" + productId + ": " + e.getMessage(), e);
            }
        }

        try {
            byte[] result = contract.submitTransaction("ShipmentContract:placeOrder", productId, quantity);
            logger.info("Service: Order placed successfully for productId={}", productId);
            return result;
        } catch (Exception e) {
            throw new FabricServiceException("Failed to place order: productId=" + productId + ": " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("squid:S1166")
    public byte[] getAuditLogByProductId(String productId) throws ContractException {
        logger.info("Service: Reading Log with product id={}", productId);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            return fetchAuditLogRemote(productId);
        }

        try {
            byte[] result = contract.evaluateTransaction("AssetContract:getAuditLogsByProductId", productId);
            logger.info("Service: Log details fetched for product id={}", productId);
            return result;
        } catch (ContractException e) {
            throw e;
        } catch (Exception e) {
            throw new ContractException(String.format("Failed to read log with product id: %s", productId) + ": " + e.getMessage(), e);
        }
    }

}
