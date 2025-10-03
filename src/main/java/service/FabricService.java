package service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;


import org.hyperledger.fabric.gateway.Contract;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class FabricService {

    private final Contract contract;

    public FabricService(Contract contract) {
        this.contract = contract;
    }

    // Query transaction
    public String createProduct(String productId, String productName, String category, String quantity) throws Exception {
        byte[] result = contract.evaluateTransaction("createProduct",productId, productName,
                category,quantity);
        return new String(result, StandardCharsets.UTF_8);
    }

    // Submit transaction
    public String queryProduct(String productId) throws Exception {
        byte[] result = contract.submitTransaction("readProduct",productId);
        return new String(result, StandardCharsets.UTF_8);
    }
}
