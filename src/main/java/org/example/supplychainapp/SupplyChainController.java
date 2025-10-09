package org.example.supplychainapp;

import org.example.supplychainapp.service.FabricService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/assets")
public class SupplyChainController {

    private final FabricService fabricService;
    private static final Logger logger = LoggerFactory.getLogger(SupplyChainController.class);

    public SupplyChainController(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    // ========================= CREATE =========================
    @PostMapping("/createProduct")
    public ResponseEntity<?> createAsset(@RequestBody Product payload) {
        logger.info("Received request to create product: id={}", payload.getProductId());
        try {
            String productId = payload.getProductId();
            String name = payload.getProductName();
            String category = payload.getCategory();
            Integer size = payload.getSize();

            fabricService.createProduct(productId, name, category, size.toString());
            return ResponseEntity.ok(Map.of(
                "message", "Product created successfully"
            ));
        } catch (Exception e) {
            logger.warn("Request failed to create product: id={}", payload.getProductId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Failed to create product"
            ));
        }
    }

    // ========================= QUERY =========================
    @GetMapping("/query/{productId}")
    public ResponseEntity<?> queryAsset(@PathVariable String productId) {
        logger.info("Received request to query product: id={}", productId);
        try {
            byte[] result = fabricService.readProduct(productId);
            String response = new String(result);
            return ResponseEntity.ok(Map.of(
                "product", response
            ));
        } catch (Exception e) {
            logger.warn("Request failed to query product: id={}", productId);
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
            String size = payload.get("size");
            fabricService.updateProductQuantity(productId, size);
            return ResponseEntity.ok(Map.of(
                "message", "Product updated successfully"
            ));
        } catch (Exception e) {
            logger.warn("Request failed to update product: id={}", productId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Failed to update product"
            ));
        }
    }

    // ========================= DELETE =========================
    @DeleteMapping("/delete/{productId}")
    public ResponseEntity<?> deleteAsset(@PathVariable String productId) {
        logger.info("Received request to delete product: id={}", productId);
        try {
            fabricService.deleteProduct(productId);
            return ResponseEntity.ok(Map.of(
                "message", "Product deleted successfully"
            ));
        } catch (Exception e) {
            logger.warn("Request failed to delete product: id={}", productId);
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
            String career = payload.get("career");
            String quantity = payload.get("quantity");

            fabricService.createShipment(shipmentId, productId, origin, destination, career, quantity);
            return ResponseEntity.ok(Map.of(
                "message", "Shipment created successfully"
            ));
        } catch (Exception e) {
            logger.warn("Request failed to create shipment: id={}", payload.get("shipmentId"));
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
            logger.warn("Request failed to query shipment: id={}", shipmentId);
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
            logger.warn("Request failed to query audit log id = {}", productId);
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
            logger.warn("Request failed to update shipment: id={}", shipmentId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Failed to update shipment"
            ));
        }
    }
}
