package org.example.supplychainapp;

public final class Constants {

    // Key names used in JSON request payloads
    public static final String SHIPMENT_ID = "shipmentId";
    public static final String PRODUCT_ID = "productId";
    public static final String QUANTITY = "quantity";

    // Common response keys
    public static final String MESSAGE = "message";
    public static final String ERROR = "error";

    // Common messages / formats
    public static final String PRODUCT_CREATED = "Product created successfully";
    public static final String PRODUCT_UPDATED = "Product updated successfully";
    public static final String PRODUCT_DELETED = "Product deleted successfully";
    public static final String SHIPMENT_CREATED = "Shipment created successfully";
    public static final String SHIPMENT_UPDATED = "Shipment updated successfully";
    public static final String FAILED_CREATE_PRODUCT = "Failed to create product";
    public static final String FAILED_UPDATE_PRODUCT = "Failed to update product";
    public static final String FAILED_DELETE_PRODUCT = "Failed to delete product";
    public static final String FAILED_CREATE_SHIPMENT = "Failed to create shipment";
    public static final String FAILED_UPDATE_SHIPMENT = "Failed to update shipment";

    public static final String PRODUCT_NOT_FOUND_FMT = "Product with id: %s is not found";
    public static final String PRODUCT_ALREADY_EXISTS_FMT = "Product with id: %s already exists";

    // Response body keys
    public static final String PRODUCT_KEY = "product";
    public static final String SHIPMENT_KEY = "shipment";

    // Other keys
    public static final String STATUS = "status";
    public static final String ORIGIN = "origin";
    public static final String DESTINATION = "destination";
    public static final String CARRIER = "carrier";

    // Role constants
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    // Additional messages
    public static final String SHIPMENT_NOT_FOUND = "Shipment not found";
    public static final String LOG_NOT_FOUND = "Log not found";
    public static final String FAILED_PLACE_ORDER = "Failed to place order";

    // Validation messages
    public static final String MISSING_PRODUCT_ID = "Missing required field: productId";

    private Constants() { /* prevent instantiation */ }
}
