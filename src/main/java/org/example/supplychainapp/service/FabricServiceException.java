package org.example.supplychainapp.service;

/**
 * Checked exception representing errors from FabricService operations.
 */
public class FabricServiceException extends Exception {
    public FabricServiceException(String message) {
        super(message);
    }

    public FabricServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

