package org.example.supplychainapp.service;

/**
 * Indicates that a product already exists.
 */
public class ProductAlreadyExistsException extends FabricServiceException {
    public ProductAlreadyExistsException(String message) {
        super(message);
    }

    public ProductAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}

