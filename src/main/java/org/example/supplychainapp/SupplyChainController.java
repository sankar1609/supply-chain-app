package org.example.supplychainapp;

import org.example.supplychainapp.service.FabricService;
import org.example.supplychainapp.service.FabricServiceException;
import org.example.supplychainapp.service.ProductAlreadyExistsException;
import org.example.supplychainapp.service.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;

@RestController
@RequestMapping("/fabric/assets")
public class SupplyChainController {

    private final FabricService fabricService;
    private static final Logger logger = LoggerFactory.getLogger(SupplyChainController.class);

    public SupplyChainController(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    // Helper to determine if an exception indicates a missing product
    private boolean isProductNotFound(Throwable e) {
        if (e == null || e.getMessage() == null) return false;
        String msg = e.getMessage().toLowerCase();
        return msg.contains("product not found") || msg.contains("not found") || msg.contains("does not exist") || msg.contains("not exist");
    }

    // Helper to determine if an exception indicates the product already exists
    private boolean isProductAlreadyExists(Throwable e) {
        if (e == null || e.getMessage() == null) return false;
        String msg = e.getMessage().toLowerCase();
        return msg.contains("already exists") || msg.contains("already exists.");
    }

    // Helper to handle HttpStatusCodeException cause and build a ResponseEntity; returns null if not applicable
    private ResponseEntity<Map<String, String>> handleHttpStatusCause(Throwable cause, String defaultMessage) {
        if (cause instanceof HttpStatusCodeException he) {
            String body = he.getResponseBodyAsString();
            var status = he.getStatusCode();
            String msg = java.util.Optional.ofNullable(body).filter(s -> !s.isBlank()).orElse(defaultMessage);
            return ResponseEntity.status(status).body(Map.of(Constants.ERROR, msg));
        }
        return null;
    }

    // Map FabricServiceException to a ResponseEntity with an appropriate status and minimal message
    private ResponseEntity<Map<String, String>> handleServiceException(FabricServiceException e, String defaultMessage, String idForNotFound) {
        // If it's a typed exception, prefer that mapping
        if (e instanceof ProductNotFoundException) {
            String id = idForNotFound == null ? "" : idForNotFound;
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    Constants.ERROR, String.format(Constants.PRODUCT_NOT_FOUND_FMT, id)
            ));
        }
        if (e instanceof ProductAlreadyExistsException) {
            String id = idForNotFound == null ? "" : idForNotFound;
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    Constants.ERROR, String.format(Constants.PRODUCT_ALREADY_EXISTS_FMT, id)
            ));
        }

        // Handle HTTP cause if present
        ResponseEntity<Map<String, String>> httpHandled = handleHttpStatusCause(e.getCause(), defaultMessage);
        if (httpHandled != null) return httpHandled;

        if (isProductNotFound(e)) {
            String id = idForNotFound == null ? "" : idForNotFound;
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    Constants.ERROR, String.format(Constants.PRODUCT_NOT_FOUND_FMT, id)
            ));
        }

        if (isProductAlreadyExists(e)) {
            String id = idForNotFound == null ? "" : idForNotFound;
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    Constants.ERROR, String.format(Constants.PRODUCT_ALREADY_EXISTS_FMT, id)
            ));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                Constants.ERROR, defaultMessage
        ));
    }

    // ========================= CREATE =========================
    @PostMapping("/createProduct")
    public ResponseEntity<Map<String, String>> createAsset(@RequestBody Product payload) {
        logger.info("Received request to create product: id={}", payload.getProductId());
        try {
            if (payload.getProductId() == null || payload.getProductId().isBlank()
                    || payload.getProductName() == null || payload.getProductName().isBlank()
                    || payload.getCategory() == null || payload.getCategory().isBlank()
                    || payload.getQuantity() == null) {
                logger.warn("Invalid createProduct request payload: {}", payload);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    Constants.ERROR, "Missing required product fields: productId, productName, category, quantity"
                ));
            }

            String productId = payload.getProductId();
            String name = payload.getProductName();
            String category = payload.getCategory();
            Integer quantity = payload.getQuantity();

            // Only invoke service when validated
            fabricService.createProduct(productId, name, category, quantity.toString());
            return ResponseEntity.ok(Map.of(
                Constants.MESSAGE, Constants.PRODUCT_CREATED
            ));
        } catch (ProductAlreadyExistsException pae) {
            logger.warn("Request failed to create product: id={} already exists", payload.getProductId());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    Constants.ERROR, String.format(Constants.PRODUCT_ALREADY_EXISTS_FMT, payload.getProductId())
            ));
        } catch (FabricServiceException e) {
            logger.warn("Request failed to create product: id={} error={}", payload.getProductId(), e.getMessage());
            return handleServiceException(e, Constants.FAILED_CREATE_PRODUCT, payload.getProductId());
        } catch (Exception e) {
            logger.warn("Request failed to create product: id={} unexpected error={}", payload.getProductId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(Constants.ERROR, Constants.FAILED_CREATE_PRODUCT));
        }
    }

    // ========================= QUERY =========================
    @GetMapping("/queryProduct/{productId}")
    public ResponseEntity<Map<String, String>> queryAsset(@PathVariable String productId) {
        logger.info("Received request to query product: id={}", productId);
        try {
            if (productId == null || productId.isBlank()) {
                logger.warn("Invalid queryProduct request: missing productId");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    Constants.ERROR, Constants.MISSING_PRODUCT_ID
                ));
            }

            byte[] result = fabricService.readProduct(productId);
            String response = new String(result);
            return ResponseEntity.ok(Map.of(
                Constants.PRODUCT_KEY, response
            ));
        } catch (ProductNotFoundException pnfe) {
            logger.warn("Request failed to query product: id={} not found", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    Constants.ERROR, String.format(Constants.PRODUCT_NOT_FOUND_FMT, productId)
            ));
        } catch (FabricServiceException e) {
            logger.warn("Request failed to query product: id={} error={}", productId, e.getMessage());
            return handleServiceException(e, "Product not found", productId);
        } catch (Exception e) {
            logger.warn("Request failed to query product: id={} unexpected error={}", productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(Constants.ERROR, "Product not found"));
        }
    }

    // ========================= UPDATE =========================
    @PutMapping("/update/{productId}")
    public ResponseEntity<Map<String, String>> updateAsset(
            @PathVariable String productId,
            @RequestBody Map<String, String> payload) {
        logger.info("Received request to update product: id={}", productId);
        try {
            if (payload == null || payload.get(Constants.QUANTITY) == null || payload.get(Constants.QUANTITY).isBlank()) {
                logger.warn("Invalid updateProduct request for id={} missing quantity", productId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    Constants.ERROR, "Missing required field: quantity"
                ));
            }

            String quantity = payload.get(Constants.QUANTITY);
            // Only invoke service when validated
            fabricService.updateProductQuantity(productId, quantity);
            return ResponseEntity.ok(Map.of(
                Constants.MESSAGE, Constants.PRODUCT_UPDATED
            ));
        } catch (ProductNotFoundException pnfe) {
            logger.warn("Request failed to update product: id={} not found", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    Constants.ERROR, String.format(Constants.PRODUCT_NOT_FOUND_FMT, productId)
            ));
        } catch (FabricServiceException e) {
            logger.warn("Request failed to update product: id={} error={}", productId, e.getMessage());
            return handleServiceException(e, Constants.FAILED_UPDATE_PRODUCT, productId);
        } catch (Exception e) {
            logger.warn("Request failed to update product: id={} unexpected error={}", productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(Constants.ERROR, Constants.FAILED_UPDATE_PRODUCT));
        }
    }

    // ========================= DELETE =========================
    @DeleteMapping("/removeProduct/{productId}")
    public ResponseEntity<Map<String, String>> deleteAsset(@PathVariable String productId) {
        logger.info("Received request to delete product: id={}", productId);
        try {
            if (productId == null || productId.isBlank()) {
                logger.warn("Invalid deleteProduct request: missing productId");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    Constants.ERROR, Constants.MISSING_PRODUCT_ID
                ));
            }

            fabricService.deleteProduct(productId);
            return ResponseEntity.ok(Map.of(
                Constants.MESSAGE, Constants.PRODUCT_DELETED
            ));
        } catch (ProductNotFoundException pnfe) {
            logger.warn("Request failed to delete product: id={} not found", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    Constants.ERROR, String.format(Constants.PRODUCT_NOT_FOUND_FMT, productId)
            ));
        } catch (FabricServiceException e) {
            logger.warn("Request failed to delete product: id={} error={}", productId, e.getMessage());
            return handleServiceException(e, Constants.FAILED_DELETE_PRODUCT, productId);
        } catch (Exception e) {
            logger.warn("Request failed to delete product: id={} unexpected error={}", productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(Constants.ERROR, Constants.FAILED_DELETE_PRODUCT));
        }
    }

    @PostMapping("/createShipment")
    public ResponseEntity<Map<String, String>> createShipment(@RequestBody Map<String, String> payload) {
        logger.info("Received request to create shipment: id={}", payload == null ? null : payload.get(Constants.SHIPMENT_ID));
        try {
            if (payload == null
                    || payload.get(Constants.SHIPMENT_ID) == null || payload.get(Constants.SHIPMENT_ID).isBlank()
                    || payload.get(Constants.PRODUCT_ID) == null || payload.get(Constants.PRODUCT_ID).isBlank()
                    || payload.get(Constants.ORIGIN) == null || payload.get(Constants.ORIGIN).isBlank()
                    || payload.get(Constants.DESTINATION) == null || payload.get(Constants.DESTINATION).isBlank()
                    || payload.get(Constants.CARRIER) == null || payload.get(Constants.CARRIER).isBlank()
                    || payload.get(Constants.QUANTITY) == null || payload.get(Constants.QUANTITY).isBlank()) {
                logger.warn("Invalid createShipment request payload: {}", payload);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    Constants.ERROR, "Missing required shipment fields: shipmentId, productId, origin, destination, carrier, quantity"
                ));
            }

            String shipmentId = payload.get(Constants.SHIPMENT_ID);
            String productId = payload.get(Constants.PRODUCT_ID);
            String origin = payload.get(Constants.ORIGIN);
            String destination = payload.get(Constants.DESTINATION);
            String carrier = payload.get(Constants.CARRIER);
            String quantity = payload.get(Constants.QUANTITY);

            // Only invoke service when validated
            fabricService.createShipment(shipmentId, productId, origin, destination, carrier, quantity);
            return ResponseEntity.ok(Map.of(
                Constants.MESSAGE, Constants.SHIPMENT_CREATED
            ));
        } catch (Exception e) {
            logger.warn("Request failed to create shipment: id={} error={}", payload == null ? "" : payload.get(Constants.SHIPMENT_ID), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                Constants.ERROR, Constants.FAILED_CREATE_SHIPMENT
            ));
        }
    }

    @GetMapping("/queryShipment/{shipmentId}")
    public ResponseEntity<Map<String, String>> queryShipment(@PathVariable String shipmentId) {
        logger.info("Received request to query shipment: id={}", shipmentId);
        try {
            if (shipmentId == null || shipmentId.isBlank()) {
                logger.warn("Invalid queryShipment request: missing shipmentId");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    Constants.ERROR, "Missing required field: shipmentId"
                ));
            }

            byte[] result = fabricService.getShipment(shipmentId);
            String response = new String(result);
            return ResponseEntity.ok(Map.of(
                Constants.SHIPMENT_KEY, response
            ));
        } catch (Exception e) {
            logger.warn("Request failed to query shipment: id={} error={}", shipmentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                Constants.ERROR, Constants.SHIPMENT_NOT_FOUND
            ));
        }
    }

    @GetMapping("/queryLogByProductId/{productId}")
    public ResponseEntity<Map<String, String>> getLogByProductId(@PathVariable String productId) {
        logger.info("Received request to query Audit Log for Product id= {}", productId);
        try {
            if (productId == null || productId.isBlank()) {
                logger.warn("Invalid queryLogByProductId request: missing productId");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    Constants.ERROR, Constants.MISSING_PRODUCT_ID
                ));
            }

            byte[] result = fabricService.getAuditLogByProductId(productId);
            String response = new String(result);
            return ResponseEntity.ok(Map.of(
                    Constants.PRODUCT_KEY, response
            ));
        } catch (Exception e) {
            logger.warn("Request failed to query audit log id = {} error={}", productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                Constants.ERROR, Constants.LOG_NOT_FOUND
            ));
        }
    }

    @PutMapping("/updateShipment/{shipmentId}")
    public ResponseEntity<Map<String, String>> updateShipment(
            @PathVariable String shipmentId,
            @RequestBody Map<String, String> payload) {
        logger.info("Received request to update shipment: id={}", shipmentId);
        try {
            if (shipmentId == null || shipmentId.isBlank()) {
                logger.warn("Invalid updateShipment request: missing shipmentId");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    Constants.ERROR, "Missing required field: shipmentId"
                ));
            }

            if (payload == null || payload.get(Constants.STATUS) == null || payload.get(Constants.STATUS).isBlank()) {
                logger.warn("Invalid updateShipment request for id={} missing status", shipmentId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    Constants.ERROR, "Missing required field: status"
                ));
            }

            String status = payload.get(Constants.STATUS);
            // Only invoke service when validated
            fabricService.updateShipmentStatus(shipmentId, status);
            return ResponseEntity.ok(Map.of(
                Constants.MESSAGE, Constants.SHIPMENT_UPDATED
            ));
        } catch (Exception e) {
            logger.warn("Request failed to update shipment: id={} error={}", shipmentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                Constants.ERROR, Constants.FAILED_UPDATE_SHIPMENT
            ));
        }
    }

    // New endpoint to place an order for a product
    @PostMapping("/placeOrder")
    public ResponseEntity<Map<String, String>> placeOrder(@RequestBody Map<String, Object> payload) {
        Object prodObj = payload == null ? null : payload.get(Constants.PRODUCT_ID);
        Object qtyObj = payload == null ? null : payload.get(Constants.QUANTITY);
        logger.info("Received request to place order: productId={}, quantity={}", prodObj, qtyObj);

        if (prodObj == null || qtyObj == null) {
            logger.warn("Invalid placeOrder request payload: {}", payload);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    Constants.ERROR, "Missing required fields: productId, quantity"
            ));
        }

        String productId = prodObj.toString();
        String quantity = qtyObj.toString();

        try {
            byte[] result = fabricService.placeOrder(productId, quantity);
            String response = new String(result);
            return ResponseEntity.ok(Map.of(
                    Constants.MESSAGE, response
            ));
        } catch (ProductNotFoundException pnfe) {
            String prodId = productId == null ? "" : productId;
            logger.warn("Request failed to place order: productId={} not found", prodId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    Constants.ERROR, String.format(Constants.PRODUCT_NOT_FOUND_FMT, prodId)
            ));
        } catch (FabricServiceException e) {
            String prodId = productId == null ? "" : productId;
            logger.warn("Request failed to place order: productId={} error={}", prodId, e.getMessage());
            return handleServiceException(e, Constants.FAILED_PLACE_ORDER, prodId);
        } catch (Exception e) {
            String prodId = productId == null ? "" : productId;
            logger.warn("Request failed to place order: productId={} unexpected error={}", prodId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(Constants.ERROR, Constants.FAILED_PLACE_ORDER));
        }
    }
}
