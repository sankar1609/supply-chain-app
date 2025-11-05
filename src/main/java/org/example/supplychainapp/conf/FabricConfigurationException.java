package org.example.supplychainapp.conf;

/**
 * Checked exception to represent errors during Fabric configuration/bootstrap.
 */
public class FabricConfigurationException extends Exception {
    public FabricConfigurationException(String message) {
        super(message);
    }

    public FabricConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}

