package org.example.supplychainapp;

import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;

public class BringCreds {
    public static void main(String[] args) throws IOException, CertificateException, InvalidKeyException {
        Path walletPath = Paths.get("wallet");
        Wallet wallet = Wallets.newFileSystemWallet(walletPath);

        Path credentialPath = Paths.get("/Users/sankarmani/Documents/ThisIsMyProject/new_fabric/fabric-samples", "test-network", "organizations", "peerOrganizations",
                "org1.example.com", "users", "User1@org1.example.com", "msp");

        Path certPath = credentialPath.resolve(Paths.get("signcerts", "cert.pem"));
        Path keyPath = Files.list(credentialPath.resolve("keystore"))
                .findFirst().get();

        Identity identity = Identities.newX509Identity("Org1MSP",
                Identities.readX509Certificate(Files.newBufferedReader(certPath)),
                Identities.readPrivateKey(Files.newBufferedReader(keyPath)));

        wallet.put("User1", identity);
    }

}
