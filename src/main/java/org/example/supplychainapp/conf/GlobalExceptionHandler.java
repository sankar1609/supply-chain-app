package org.example.supplychainapp.conf;

import org.example.supplychainapp.Constants;
import org.example.supplychainapp.service.FabricServiceException;
import org.example.supplychainapp.service.ProductNotFoundException;
import org.example.supplychainapp.service.ProductAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FabricConfigurationException.class)
    public ResponseEntity<Map<String, String>> handleFabricConfig(FabricConfigurationException ex) {
        logger.error("Fabric configuration error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                Constants.ERROR, "Internal server error: fabric configuration"
        ));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleProductNotFound(ProductNotFoundException ex) {
        logger.warn("Product not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(Constants.ERROR, ex.getMessage()));
    }

    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleProductAlreadyExists(ProductAlreadyExistsException ex) {
        logger.warn("Product already exists: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(Constants.ERROR, ex.getMessage()));
    }

    @ExceptionHandler(FabricServiceException.class)
    public ResponseEntity<Map<String, String>> handleFabricService(FabricServiceException ex) {
        logger.warn("Unhandled FabricServiceException: {}", ex.getMessage());
        String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (msg.contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(Constants.ERROR, ex.getMessage()));
        }
        if (msg.contains("already exists")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(Constants.ERROR, ex.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(Constants.ERROR, ex.getMessage()));
    }
}
