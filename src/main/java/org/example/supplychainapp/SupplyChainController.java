package org.example.supplychainapp;

import org.hyperledger.fabric.gateway.Contract;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assets")
public class SupplyChainController {

    private final Contract contract;

    public SupplyChainController(Contract contract) {
        this.contract = contract;
    }

    // ========================= CREATE =========================
    @PostMapping("/createProduct")
    public ResponseEntity<String> createAsset(@RequestBody Product payload) {
        try {
            String productId = payload.getProductId();
            String name = payload.getProductName();
            String category = payload.getCategory();
            Integer size = payload.getSize();

            byte[] result = contract.submitTransaction("AssetContract:createProduct", productId, name, category, size.toString());
            return ResponseEntity.ok(new String(result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // ========================= QUERY =========================
    @GetMapping("/query/{productId}")
    public ResponseEntity<String> queryAsset(@PathVariable String productId) {
        try {
            byte[] result = contract.evaluateTransaction("AssetContract:readProduct", productId);
            return ResponseEntity.ok(new String(result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // ========================= UPDATE =========================
    @PutMapping("/update/{productId}")
    public ResponseEntity<String> updateAsset(
            @PathVariable String productId,
            @RequestBody Map<String, String> payload) {
        try {
            //String producName = payload.get("producName");
            String size = payload.get("size");

            byte[] result = contract.submitTransaction("AssetContract:updateProductQuantity", productId, size);
            return ResponseEntity.ok(new String(result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // ========================= DELETE =========================
    @DeleteMapping("/delete/{productId}")
    public ResponseEntity<String> deleteAsset(@PathVariable String productId) {
        try {
            byte[] result = contract.submitTransaction("AssetContract:deleteProduct", productId);
            return ResponseEntity.ok(new String(result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    @PostMapping("/createShipment")
    public ResponseEntity<String> createShipment(@RequestBody Map<String, String> payload) {
        try {
            String shipmentId = payload.get("shipmentId");
            String productId = payload.get("productId");
            String origin = payload.get("origin");
            String destination = payload.get("destination");
            String career = payload.get("career");

            byte[] result = contract.submitTransaction("ShipmentContract:createShipment", shipmentId, productId, origin,destination, career);
            return ResponseEntity.ok(new String(result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    @GetMapping("/queryShipment/{shipmentId}")
    public ResponseEntity<String> queryShipment(@PathVariable String shipmentId) {
        try {
            byte[] result = contract.evaluateTransaction("ShipmentContract:getShipment", shipmentId);
            return ResponseEntity.ok(new String(result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    @PutMapping("/updateShipment/{shipmentId}")
    public ResponseEntity<String> updateShipment(
            @PathVariable String shipmentId,
            @RequestBody Map<String, String> payload) {
        try {
            //String producName = payload.get("producName");
            String status = payload.get("status");

            byte[] result = contract.submitTransaction("ShipmentContract:updateShipmentStatus", shipmentId, status);
            return ResponseEntity.ok(new String(result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
