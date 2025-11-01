package org.example.supplychainapp;

import org.example.supplychainapp.service.FabricService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // ========================= CREATE =========================
    @PostMapping("/createProduct")
    public ResponseEntity<?> createAsset(@RequestBody Product payload) {
        logger.info("Received request to create product: id={}", payload.getProductId());
        try {
            String productId = payload.getProductId();
            String name = payload.getProductName();
            String category = payload.getCategory();
            Integer quantity = payload.getQuantity();

            fabricService.createProduct(productId, name, category, quantity.toString());
            return ResponseEntity.ok(Map.of(
                "message", "Product created successfully"
            ));
        } catch (Exception e) {
            logger.warn("Request failed to create product: id={} error={}", payload.getProductId(), e.getMessage());
            // If the error indicates the product already exists, return a clear message
            if (isProductAlreadyExists(e)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", String.format("Product with id: %s already exists", payload.getProductId())
                ));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Failed to create product"
            ));
        }
    }

    // ========================= QUERY =========================
    @GetMapping("/queryProduct/{productId}")
    public ResponseEntity<?> queryAsset(@PathVariable String productId) {
        logger.info("Received request to query product: id={}", productId);
        try {
            byte[] result = fabricService.readProduct(productId);
            String response = new String(result);
            return ResponseEntity.ok(Map.of(
                "product", response
            ));
        } catch (Exception e) {
            logger.warn("Request failed to query product: id={} error={}", productId, e.getMessage());
            // Return specific not-found message if applicable
            if (isProductNotFound(e)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", String.format("Product with id: %s is not found", productId)
                ));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Product not found"
            ));
        }
    }

    // ========================= UPDATE =========================
    @PutMapping("/update/{productId}")
    public ResponseEntity<?> updateAsset(
            @PathVariable String productId,
            @RequestBody Map<String, String> payload) {
        logger.info("Received request to update product: id={}", productId);
        try {
            String quantity = payload.get("quantity");
            fabricService.updateProductQuantity(productId, quantity);
            return ResponseEntity.ok(Map.of(
                "message", "Product updated successfully"
            ));
        } catch (Exception e) {
            logger.warn("Request failed to update product: id={} error={}", productId, e.getMessage());
            if (isProductNotFound(e)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", String.format("Product with id: %s is not found", productId)
                ));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Failed to update product"
            ));
        }
    }

    // ========================= DELETE =========================
    @DeleteMapping("/removeProduct/{productId}")
    public ResponseEntity<?> deleteAsset(@PathVariable String productId) {
        logger.info("Received request to delete product: id={}", productId);
        try {
            fabricService.deleteProduct(productId);
            return ResponseEntity.ok(Map.of(
                "message", "Product deleted successfully"
            ));
        } catch (Exception e) {
            logger.warn("Request failed to delete product: id={} error={}", productId, e.getMessage());
            if (isProductNotFound(e)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", String.format("Product with id: %s is not found", productId)
                ));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Failed to delete product"
            ));
        }
    }

    @PostMapping("/createShipment")
    public ResponseEntity<?> createShipment(@RequestBody Map<String, String> payload) {
        logger.info("Received request to create shipment: id={}", payload.get("shipmentId"));
        try {
            String shipmentId = payload.get("shipmentId");
            String productId = payload.get("productId");
            String origin = payload.get("origin");
            String destination = payload.get("destination");
            String carrier = payload.get("carrier");
            String quantity = payload.get("quantity");

            fabricService.createShipment(shipmentId, productId, origin, destination, carrier, quantity);
            return ResponseEntity.ok(Map.of(
                "message", "Shipment created successfully"
            ));
        } catch (Exception e) {
            logger.warn("Request failed to create shipment: id={} error={}", payload.get("shipmentId"), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Failed to create shipment"
            ));
        }
    }

    @GetMapping("/queryShipment/{shipmentId}")
    public ResponseEntity<?> queryShipment(@PathVariable String shipmentId) {
        logger.info("Received request to query shipment: id={}", shipmentId);
        try {
            byte[] result = fabricService.getShipment(shipmentId);
            String response = new String(result);
            return ResponseEntity.ok(Map.of(
                "shipment", response
            ));
        } catch (Exception e) {
            logger.warn("Request failed to query shipment: id={} error={}", shipmentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Shipment not found"
            ));
        }
    }

    @GetMapping("/queryLogByProductId/{productId}")
    public ResponseEntity<?> getLogByProductId(@PathVariable String productId) {
        logger.info("Received request to query Audit Log for Product id= {}", productId);
        try {
            byte[] result = fabricService.getAuditLogByProductId(productId);
            String response = new String(result);
            return ResponseEntity.ok(Map.of(
                    "product", response
            ));
        } catch (Exception e) {
            logger.warn("Request failed to query audit log id = {} error={}", productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Log not found"
            ));
        }
    }

    @PutMapping("/updateShipment/{shipmentId}")
    public ResponseEntity<?> updateShipment(
            @PathVariable String shipmentId,
            @RequestBody Map<String, String> payload) {
        logger.info("Received request to update shipment: id={}", shipmentId);
        try {
            String status = payload.get("status");
            fabricService.updateShipmentStatus(shipmentId, status);
            return ResponseEntity.ok(Map.of(
                "message", "Shipment updated successfully"
            ));
        } catch (Exception e) {
            logger.warn("Request failed to update shipment: id={} error={}", shipmentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Failed to update shipment"
            ));
        }
    }

    // New endpoint to place an order for a product
    @PostMapping("/placeOrder")
    public ResponseEntity<Map<String, String>> placeOrder(@RequestBody Map<String, Object> payload) {
        logger.info("Received request to place order: productId={}, quantity={}", payload.get("productId"), payload.get("quantity"));
        try {
            String productId = payload.get("productId").toString();
            String quantity = payload.get("quantity").toString();
            byte[] result = fabricService.placeOrder(productId, quantity);
            String response = new String(result);
            return ResponseEntity.ok(Map.of(
                "message", response
            ));
        } catch (Exception e) {
            String prodId = payload.get("productId") == null ? "" : payload.get("productId").toString();
            logger.warn("Request failed to place order: productId={} error={}", prodId, e.getMessage());
            if (isProductNotFound(e)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", String.format("Product with id: %s is not found", prodId)
                ));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Failed to place order"
            ));
        }
    }
}
