package org.example.supplychainapp.conf;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.nio.file.Paths;

@Configuration
public class FabricConfig {

    @Value("classpath:networkConfig.yaml")
    private Resource networkConfig;

    @Bean
    public Gateway gateway() throws Exception {
        Wallet wallet = Wallets.newFileSystemWallet(Paths.get("wallet"));

        Gateway.Builder builder = Gateway.createBuilder()
                .identity(wallet, "User1")
                .networkConfig(networkConfig.getFile().toPath())
                .discovery(false);

        return builder.connect();
    }

    @Bean
    public Network network(Gateway gateway) {
        return gateway.getNetwork("supplychainchannel");
    }

    @Bean
    public Contract contract(Network network) {
        return network.getContract("SupplyChainContract");
    }
}

