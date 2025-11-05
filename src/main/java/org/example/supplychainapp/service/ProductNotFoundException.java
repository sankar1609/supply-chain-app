package org.example.supplychainapp.service;

/**
 * Indicates that a product was not found in Fabric or remote service.
 */
public class ProductNotFoundException extends FabricServiceException {
    public ProductNotFoundException(String message) {
        super(message);
    }

    public ProductNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

