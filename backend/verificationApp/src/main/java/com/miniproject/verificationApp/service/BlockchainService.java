package com.miniproject.verificationApp.service; 
 
import jakarta.annotation.PostConstruct; 
import jakarta.annotation.PreDestroy; 
import org.slf4j.Logger; 
import org.slf4j.LoggerFactory; 
import org.springframework.beans.factory.annotation.Value; 
import org.springframework.scheduling.annotation.Async; 
import org.springframework.stereotype.Service; 
 
import org.web3j.protocol.Web3j; 
import org.web3j.protocol.http.HttpService; 
import org.web3j.crypto.Credentials; 
import org.web3j.tx.RawTransactionManager; 
import org.web3j.tx.gas.DefaultGasProvider; 
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt; 
import org.web3j.protocol.core.methods.response.EthSendTransaction; 
import org.web3j.protocol.core.methods.response.TransactionReceipt; 
 
import org.web3j.abi.EventEncoder; 
import org.web3j.abi.datatypes.Function; 
import org.web3j.abi.FunctionEncoder; 
import org.web3j.abi.FunctionReturnDecoder; 
import org.web3j.abi.TypeReference; 
import org.web3j.abi.datatypes.Address; 
import org.web3j.abi.datatypes.Event; 
import org.web3j.abi.datatypes.Type; 
import org.web3j.abi.datatypes.Utf8String; 
import org.web3j.abi.datatypes.Bool; 
import org.web3j.abi.datatypes.generated.Uint8; 
import org.web3j.abi.datatypes.generated.Uint256; 
 
import java.math.BigInteger; 
import java.util.Arrays; 
import java.util.List; 
import java.util.Optional; 
import java.util.concurrent.CompletableFuture; 
 
@Service 
public class BlockchainService { 
 
    private static final Logger logger = 
            LoggerFactory.getLogger(BlockchainService.class); 
 
    private static final Event REVIEW_STORED_EVENT = 
            new Event( 
                    "ReviewStored", 
                    Arrays.asList( 
                            TypeReference.create(Utf8String.class), 
                            TypeReference.create(Utf8String.class), 
                            TypeReference.create(Uint8.class), 
                            TypeReference.create(Uint8.class), 
                            TypeReference.create(Bool.class), 
                            TypeReference.create(Address.class), 
                            TypeReference.create(Uint256.class) 
                    ) 
            ); 
 
    @Value("${blockchain.infura.url}") 
    private String infuraUrl; 
 
    @Value("${blockchain.private.key}") 
    private String privateKey; 
 
    @Value("${blockchain.contract.address}") 
    private String contractAddress; 
 
    private Web3j web3j; 
    private RawTransactionManager txManager; 
 
    @PostConstruct 
    void initializeClient() { 
        try { 
            web3j = Web3j.build(new HttpService(infuraUrl)); 
            Credentials credentials = Credentials.create(privateKey); 

            logger.info("Backend blockchain wallet address = {}", credentials.getAddress());

            txManager = new RawTransactionManager(web3j, credentials, 11155111L);
        } catch (Exception e) { 
            web3j = null; 
            txManager = null; 
            logger.warn( 
                    "Blockchain client initialization failed exceptionType={}", 
                    e.getClass().getName() 
            ); 
        } 
    } 
 
    @PreDestroy 
    void closeClient() { 
        if (web3j != null) { 
            web3j.shutdown(); 
        } 
    } 
 
    @Async 
    public CompletableFuture<String> storeHash( 
            String hash, 
            String place, 
            int aiScore, 
            int nlpScore, 
            boolean gpsVerified 
    ) { 
 
        try { 
            if (txManager == null) { 
                return CompletableFuture.completedFuture(null); 
            } 
 
            Function function = 
                    new Function( 
                            "storeReview", 
 
                            Arrays.asList( 
 
                                    new Utf8String(hash), 
 
                                    new Utf8String(place), 
 
                                    new Uint8(aiScore), 
 
                                    new Uint8(nlpScore), 
 
                                    new Bool(gpsVerified) 
                            ), 
 
                            Arrays.asList() 
                    ); 
 
            String encodedFunction = 
                    FunctionEncoder.encode( 
                            function 
                    ); 
 
            EthSendTransaction response = 
                    txManager.sendTransaction( 
 
                            DefaultGasProvider.GAS_PRICE, 
 
                            DefaultGasProvider.GAS_LIMIT, 
 
                            contractAddress, 
 
                            encodedFunction, 
 
                            BigInteger.ZERO 
                    ); 

            if (response.hasError()) {
                logger.error(
                        "Blockchain error: {}",
                        response.getError().getMessage()
                );
                return CompletableFuture.completedFuture(null);
            }
 
            String txHash = 
                    response.getTransactionHash(); 
 
            if (txHash != null) { 
                logger.info("TX HASH: {}", txHash); 
 
                int attempts = 0; 
                int maxAttempts = 20; 
 
                while (attempts < maxAttempts) { 
 
                    Thread.sleep(3000); 
 
                    EthGetTransactionReceipt receipt = web3j 
                            .ethGetTransactionReceipt(txHash) 
                            .send(); 
 
                    if (receipt.getTransactionReceipt().isPresent()) { 
 
                        TransactionReceipt txReceipt = 
                                receipt.getTransactionReceipt().get(); 
 
                        if (txReceipt.isStatusOK()) { 
                            logger.info("Blockchain transaction status=CONFIRMED"); 
                            return CompletableFuture.completedFuture(txHash); 
                        } 
                        return CompletableFuture.completedFuture(null); 
                    } 
 
                    attempts++; 
                } 
            } 
 
            return CompletableFuture.completedFuture(null); 
 
        } catch (Exception e) { 
            logger.error( 
                    "Blockchain transaction failed exceptionType={}", 
                    e.getClass().getName() 
            ); 
            return CompletableFuture.completedFuture(null); 
        } 
    } 
 
    public Optional<String> getStoredReviewHash(String txHash) { 
        if (txHash == null 
                || txHash.isBlank() 
                || "pending".equalsIgnoreCase(txHash) 
                || web3j == null) { 
            return Optional.empty(); 
        } 
 
        try { 
            EthGetTransactionReceipt receipt = web3j 
                    .ethGetTransactionReceipt(txHash) 
                    .send(); 
 
            if (receipt.getTransactionReceipt().isEmpty()) { 
                return Optional.empty(); 
            } 
 
            TransactionReceipt txReceipt = 
                    receipt.getTransactionReceipt().get(); 
 
            if (!txReceipt.isStatusOK()) { 
                return Optional.empty(); 
            } 
 
            String reviewStoredTopic = 
                    EventEncoder.encode(REVIEW_STORED_EVENT); 
 
            for (org.web3j.protocol.core.methods.response.Log log 
                    : txReceipt.getLogs()) { 
 
                if (contractAddress == null 
                        || log.getAddress() == null 
                        || !contractAddress.equalsIgnoreCase(log.getAddress())) { 
                    continue; 
                } 
 
                if (log.getTopics() == null 
                        || log.getTopics().isEmpty() 
                        || !reviewStoredTopic.equalsIgnoreCase( 
                                log.getTopics().get(0) 
                        )) { 
                    continue; 
                } 
 
                List<Type> decoded = 
                        FunctionReturnDecoder.decode( 
                                log.getData(), 
                                REVIEW_STORED_EVENT.getNonIndexedParameters() 
                        ); 
 
                if (!decoded.isEmpty()) { 
                    return Optional.of((String) decoded.get(0).getValue()); 
                } 
            } 
 
            return Optional.empty(); 
 
        } catch (Exception e) { 
            logger.warn( 
                    "Blockchain review hash lookup failed exceptionType={}", 
                    e.getClass().getName() 
            ); 
            return Optional.empty(); 
        } 
    } 
}