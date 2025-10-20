package org.example.supplychainapp.service;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FabricService {
    private final Contract contract;
    private static final Logger logger = LoggerFactory.getLogger(FabricService.class);

    public FabricService(Contract contract) {
        this.contract = contract;
    }

    public byte[] createProduct(String productId, String name, String category, String quantity) throws Exception {
        logger.info("Service: Creating product with id={}, name={}, category={}, quantity={}", productId, name, category, quantity);
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
        try {
            byte[] result = contract.submitTransaction("ShipmentContract:updateShipmentStatus", shipmentId, status);
            logger.info("Service: Shipment updated successfully: id={}", shipmentId);
            return result;
        } catch (Exception e) {
            logger.error("Service: Failed to update shipment: id={}, error={}", shipmentId, e.getMessage());
            throw e;
        }
    }

    public byte[] getAuditLogByProductId(String productId) throws ContractException {
        logger.info("Service: Reading Log with product id={}", productId);
        try {
            byte[] result = contract.evaluateTransaction("AssetContract:getAuditLogsByProductId", productId);
            logger.info("Service: Log details fetched for product id={}", productId);
            return result;
        } catch (Exception e) {
            logger.error("Service: Failed to read log with product id={}, error={}", productId, e.getMessage());
            throw e;
        }
    }
}
