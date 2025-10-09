package org.example.supplychainapp.service;

import org.hyperledger.fabric.gateway.Contract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FabricServiceTest {
    private Contract contractMock;
    private FabricService fabricService;

    @BeforeEach
    void setUp() {
        contractMock = Mockito.mock(Contract.class);
        fabricService = new FabricService(contractMock);
    }

    @Test
    @DisplayName("createProduct returns expected result on success")
    void createProductReturnsExpectedResult() throws Exception {
        byte[] expected = "created".getBytes();
        when(contractMock.submitTransaction(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(expected);
        byte[] result = fabricService.createProduct("1", "name", "cat", "size");
        assertArrayEquals(expected, result);
    }

    @Test
    @DisplayName("createProduct throws exception on failure")
    void createProductThrowsException() throws Exception {
        when(contractMock.submitTransaction(anyString(), anyString(), anyString(), anyString(), anyString())).thenThrow(new RuntimeException("fail"));
        Exception ex = assertThrows(Exception.class, () -> fabricService.createProduct("1", "name", "cat", "size"));
        assertEquals("fail", ex.getMessage());
    }

    @Test
    @DisplayName("readProduct returns expected result on success")
    void readProductReturnsExpectedResult() throws Exception {
        byte[] expected = "read".getBytes();
        when(contractMock.evaluateTransaction(anyString(), anyString())).thenReturn(expected);
        byte[] result = fabricService.readProduct("1");
        assertArrayEquals(expected, result);
    }

    @Test
    @DisplayName("updateProductQuantity returns expected result on success")
    void updateProductQuantityReturnsExpectedResult() throws Exception {
        byte[] expected = "updated".getBytes();
        when(contractMock.submitTransaction(anyString(), anyString(), anyString())).thenReturn(expected);
        byte[] result = fabricService.updateProductQuantity("1", "10");
        assertArrayEquals(expected, result);
    }

    @Test
    @DisplayName("deleteProduct returns expected result on success")
    void deleteProductReturnsExpectedResult() throws Exception {
        byte[] expected = "deleted".getBytes();
        when(contractMock.submitTransaction(anyString(), anyString())).thenReturn(expected);
        byte[] result = fabricService.deleteProduct("1");
        assertArrayEquals(expected, result);
    }

    @Test
    @DisplayName("createShipment returns expected result on success")
    void createShipmentReturnsExpectedResult() throws Exception {
        byte[] expected = "shipmentCreated".getBytes();
        when(contractMock.submitTransaction(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(expected);
        byte[] result = fabricService.createShipment("sid", "pid",
                "origin", "dest", "career","quantity");
        assertArrayEquals(expected, result);
    }

    @Test
    @DisplayName("getShipment returns expected result on success")
    void getShipmentReturnsExpectedResult() throws Exception {
        byte[] expected = "shipment".getBytes();
        when(contractMock.evaluateTransaction(anyString(), anyString())).thenReturn(expected);
        byte[] result = fabricService.getShipment("sid");
        assertArrayEquals(expected, result);
    }

    @Test
    @DisplayName("updateShipmentStatus returns expected result on success")
    void updateShipmentStatusReturnsExpectedResult() throws Exception {
        byte[] expected = "statusUpdated".getBytes();
        when(contractMock.submitTransaction(anyString(), anyString(), anyString())).thenReturn(expected);
        byte[] result = fabricService.updateShipmentStatus("sid", "status");
        assertArrayEquals(expected, result);
    }
}

