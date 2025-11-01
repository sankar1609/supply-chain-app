package org.example.supplychainapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FabricService {
    private final Contract contract;
    private static final Logger logger = LoggerFactory.getLogger(FabricService.class);
    private final boolean remoteEnabled;
    private final boolean remoteUseEureka;
    private final String remoteUrl;
    private final String remoteServiceId;
    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FabricService(Contract contract,
                         @Value("${supplychain.remote.enabled:false}") boolean remoteEnabled,
                         @Value("${supplychain.remote.use-eureka:true}") boolean remoteUseEureka,
                         @Value("${supplychain.remote.url:}") String remoteUrl,
                         @Value("${supplychain.remote.serviceId:supplychain-service}") String remoteServiceId,
                         RestTemplate restTemplate,
                         DiscoveryClient discoveryClient) {
        this.contract = contract;
        this.remoteEnabled = remoteEnabled;
        this.remoteUseEureka = remoteUseEureka;
        this.remoteUrl = remoteUrl != null ? remoteUrl.replaceAll("/+$", "") : "";
        this.remoteServiceId = remoteServiceId;
        this.restTemplate = restTemplate;
        this.discoveryClient = discoveryClient;
    }

    private String resolveRemoteBaseUrl() {
        if (remoteUseEureka && discoveryClient != null) {
            try {
                List<ServiceInstance> instances = discoveryClient.getInstances(remoteServiceId);
                if (instances != null && !instances.isEmpty()) {
                    ServiceInstance inst = instances.get(0);
                    String scheme = inst.isSecure() ? "https" : "http";
                    return String.format("%s://%s:%d", scheme, inst.getHost(), inst.getPort());
                }
            } catch (Exception e) {
                logger.warn("Failed to resolve remote service via discovery: {}", e.getMessage());
            }
        }
        return remoteUrl;
    }

    // Helper to choose remote or local invocation
    private byte[] callRemoteOrLocal(String localMethod, RemoteCall remoteCall) throws Exception {
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            try {
                return remoteCall.call();
            } catch (HttpStatusCodeException he) {
                // Return a concise message for end users
                String msg = String.format("Remote call failed: %s", he.getResponseBodyAsString());
                logger.warn(msg);
                throw new Exception(msg);
            } catch (Exception e) {
                logger.warn("Remote call failed: {}", e.getMessage());
                throw e;
            }
        }

        // Fallback to local contract invocation
        try {
            switch (localMethod) {
                case "createProduct":
                    // handled in caller
                    throw new UnsupportedOperationException("localMethod should be handled in caller");
                default:
                    throw new UnsupportedOperationException("Unsupported local method: " + localMethod);
            }
        } catch (Exception e) {
            logger.error("Local fabric invocation failed: {}", e.getMessage());
            throw e;
        }
    }

    // Functional interface for remote calls
    private interface RemoteCall {
        byte[] call() throws Exception;
    }

    public byte[] createProduct(String productId, String name, String category, String quantity) throws Exception {
        logger.info("Service: Creating product with id={}, name={}, category={}, quantity={}", productId, name, category, quantity);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            String url = remoteUrl + "/fabric/assets/createProduct";
            Map<String, Object> payload = new HashMap<>();
            payload.put("productId", productId);
            payload.put("productName", name);
            payload.put("category", category);
            payload.put("quantity", Integer.parseInt(quantity));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            try {
                ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
                logger.info("Service: Remote product create returned status={}", resp.getStatusCode());
                // Extract essential message for end user
                Map<?,?> map = objectMapper.readValue(resp.getBody(), Map.class);
                String message = map.containsKey("message") ? map.get("message").toString() : resp.getBody();
                return message.getBytes();
            } catch (HttpStatusCodeException he) {
                String body = he.getResponseBodyAsString();
                logger.error("Service: Remote createProduct failed: {}", body);
                throw new Exception("Failed to create product: " + body);
            }
        }

        try {
            byte[] result = contract.submitTransaction("AssetContract:createProduct", productId, name, category, quantity);
            logger.info("Service: Product created successfully: id={}", productId);
            return result;
        } catch (Exception e) {
            logger.error("Service: Failed to create product: id={}, error={}", productId, e.getMessage());
            throw e;
        }
    }

    public byte[] readProduct(String productId) throws Exception {
        logger.info("Service: Reading product with id={}", productId);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            String url = String.format("%s/fabric/assets/queryProduct/%s", remoteUrl, productId);
            try {
                ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
                Map<?,?> map = objectMapper.readValue(resp.getBody(), Map.class);
                Object product = map.containsKey("product") ? map.get("product") : resp.getBody();
                return product.toString().getBytes();
            } catch (HttpStatusCodeException he) {
                String body = he.getResponseBodyAsString();
                logger.error("Service: Remote readProduct failed: {}", body);
                throw new Exception("Product not found: " + body);
            }
        }

        try {
            byte[] result = contract.evaluateTransaction("AssetContract:readProduct", productId);
            logger.info("Service: Product details fetched: id={}", productId);
            return result;
        } catch (Exception e) {
            logger.error("Service: Failed to read product: id={}, error={}", productId, e.getMessage());
            throw e;
        }
    }

    public byte[] updateProductQuantity(String productId, String quantity) throws Exception {
        logger.info("Service: Updating product quantity: id={}, new quantity={}", productId, quantity);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            String url = String.format("%s/fabric/assets/update/%s", remoteUrl, productId);
            Map<String, String> payload = new HashMap<>();
            payload.put("quantity", quantity);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            try {
                ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
                Map<?,?> map = objectMapper.readValue(resp.getBody(), Map.class);
                String message = map.containsKey("message") ? map.get("message").toString() : resp.getBody();
                return message.getBytes();
            } catch (HttpStatusCodeException he) {
                String body = he.getResponseBodyAsString();
                logger.error("Service: Remote updateProduct failed: {}", body);
                throw new Exception("Failed to update product: " + body);
            }
        }

        try {
            byte[] result = contract.submitTransaction("AssetContract:updateProductQuantity", productId, quantity);
            logger.info("Service: Product updated successfully: id={}", productId);
            return result;
        } catch (Exception e) {
            logger.error("Service: Failed to update product: id={}, error={}", productId, e.getMessage());
            throw e;
        }
    }

    public byte[] deleteProduct(String productId) throws Exception {
        logger.info("Service: Deleting product with id={}", productId);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            String url = String.format("%s/fabric/assets/removeProduct/%s", remoteUrl, productId);
            try {
                ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.DELETE, null, String.class);
                Map<?,?> map = objectMapper.readValue(resp.getBody(), Map.class);
                String message = map.containsKey("message") ? map.get("message").toString() : resp.getBody();
                return message.getBytes();
            } catch (HttpStatusCodeException he) {
                String body = he.getResponseBodyAsString();
                logger.error("Service: Remote deleteProduct failed: {}", body);
                throw new Exception("Failed to delete product: " + body);
            }
        }

        try {
            byte[] result = contract.submitTransaction("AssetContract:deleteProduct", productId);
            logger.info("Service: Product deleted successfully: id={}", productId);
            return result;
        } catch (Exception e) {
            logger.error("Service: Failed to delete product: id={}, error={}", productId, e.getMessage());
            throw e;
        }
    }

    public byte[] createShipment(String shipmentId, String productId, String origin, String destination, String carrier,
                                 String quantity) throws Exception {
        logger.info("Service: Creating shipment: id={}, productId={}, origin={}, destination={}, carrier={}, quantity{}",
                shipmentId, productId, origin, destination, carrier, quantity);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            String url = remoteUrl + "/fabric/assets/createShipment";
            Map<String, Object> payload = new HashMap<>();
            payload.put("shipmentId", shipmentId);
            payload.put("productId", productId);
            payload.put("origin", origin);
            payload.put("destination", destination);
            payload.put("carrier", carrier);
            payload.put("quantity", Integer.parseInt(quantity));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            try {
                ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
                Map<?,?> map = objectMapper.readValue(resp.getBody(), Map.class);
                String message = map.containsKey("message") ? map.get("message").toString() : resp.getBody();
                return message.getBytes();
            } catch (HttpStatusCodeException he) {
                String body = he.getResponseBodyAsString();
                logger.error("Service: Remote createShipment failed: {}", body);
                throw new Exception("Failed to create shipment: " + body);
            }
        }

        try {
            byte[] result = contract.submitTransaction("ShipmentContract:createShipment", shipmentId, productId, origin, destination,
                    carrier, quantity);
            logger.info("Service: Shipment created successfully: id={}", shipmentId);
            return result;
        } catch (Exception e) {
            logger.error("Service: Failed to create shipment: id={}, error={}", shipmentId, e.getMessage());
            throw e;
        }
    }

    public byte[] getShipment(String shipmentId) throws Exception {
        logger.info("Service: Reading shipment with id={}", shipmentId);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            String url = String.format("%s/fabric/assets/queryShipment/%s", remoteUrl, shipmentId);
            try {
                ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
                Map<?,?> map = objectMapper.readValue(resp.getBody(), Map.class);
                Object shipment = map.containsKey("shipment") ? map.get("shipment") : resp.getBody();
                return shipment.toString().getBytes();
            } catch (HttpStatusCodeException he) {
                String body = he.getResponseBodyAsString();
                logger.error("Service: Remote getShipment failed: {}", body);
                throw new Exception("Shipment not found: " + body);
            }
        }

        try {
            byte[] result = contract.evaluateTransaction("ShipmentContract:getShipment", shipmentId);
            logger.info("Service: Shipment details fetched: id={}", shipmentId);
            return result;
        } catch (Exception e) {
            logger.error("Service: Failed to read shipment: id={}, error={}", shipmentId, e.getMessage());
            throw e;
        }
    }

    public byte[] updateShipmentStatus(String shipmentId, String status) throws Exception {
        logger.info("Service: Updating shipment status: id={}, new status={}", shipmentId, status);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            String url = String.format("%s/fabric/assets/updateShipment/%s", remoteUrl, shipmentId);
            Map<String, String> payload = new HashMap<>();
            payload.put("status", status);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            try {
                ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
                Map<?,?> map = objectMapper.readValue(resp.getBody(), Map.class);
                String message = map.containsKey("message") ? map.get("message").toString() : resp.getBody();
                return message.getBytes();
            } catch (HttpStatusCodeException he) {
                String body = he.getResponseBodyAsString();
                logger.error("Service: Remote updateShipment failed: {}", body);
                throw new Exception("Failed to update shipment: " + body);
            }
        }

        try {
            byte[] result = contract.submitTransaction("ShipmentContract:updateShipmentStatus", shipmentId, status);
            logger.info("Service: Shipment updated successfully: id={}", shipmentId);
            return result;
        } catch (Exception e) {
            logger.error("Service: Failed to update shipment: id={}, error={}", shipmentId, e.getMessage());
            throw e;
        }
    }

    // New method to place an order (calls chaincode 'placeOrder')
    public byte[] placeOrder(String productId, String quantity) throws Exception {
        logger.info("Service: Placing order for productId={}, quantity={}", productId, quantity);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            String url = remoteUrl + "/fabric/assets/placeOrder";
            Map<String, Object> payload = new HashMap<>();
            payload.put("productId", productId);
            payload.put("quantity", Integer.parseInt(quantity));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            try {
                ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
                Map<?,?> map = objectMapper.readValue(resp.getBody(), Map.class);
                String message = map.containsKey("message") ? map.get("message").toString() : resp.getBody();
                return message.getBytes();
            } catch (HttpStatusCodeException he) {
                String body = he.getResponseBodyAsString();
                logger.error("Service: Remote placeOrder failed: {}", body);
                throw new Exception("Failed to place order: " + body);
            }
        }

        try {
            // Assumption: the chaincode namespace for product operations is AssetContract
            byte[] result = contract.submitTransaction("ShipmentContract:placeOrder", productId, quantity);
            logger.info("Service: Order placed successfully for productId={}", productId);
            return result;
        } catch (Exception e) {
            logger.error("Service: Failed to place order: productId={}, error={}", productId, e.getMessage());
            throw e;
        }
    }

    public byte[] getAuditLogByProductId(String productId) throws ContractException {
        logger.info("Service: Reading Log with product id={}", productId);
        if (remoteEnabled && remoteUrl != null && !remoteUrl.isBlank()) {
            String url = String.format("%s/fabric/assets/queryLogByProductId/%s", remoteUrl, productId);
            try {
                ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
                Map<?,?> map = objectMapper.readValue(resp.getBody(), Map.class);
                Object product = map.containsKey("product") ? map.get("product") : resp.getBody();
                return product.toString().getBytes();
            } catch (HttpStatusCodeException he) {
                String body = he.getResponseBodyAsString();
                logger.error("Service: Remote getAuditLogByProductId failed: {}", body);
                throw new ContractException("Log not found: " + body);
            } catch (Exception e) {
                logger.error("Service: Failed to read log with product id={}, error={}", productId, e.getMessage());
                throw new ContractException(e.getMessage());
            }
        }

        try {
            byte[] result = contract.evaluateTransaction("AssetContract:getAuditLogsByProductId", productId);
            logger.info("Service: Log details fetched for product id={}", productId);
            return result;
        } catch (ContractException e) {
            logger.error("Service: Failed to read log with product id={}, error={}", productId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Service: Failed to read log with product id={}, error={}", productId, e.getMessage());
            throw new ContractException(e.getMessage());
        }
    }
}
