package dev.mouradski.sgbnftbot.service;

import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class EthHelper {

    private Web3j web3;

    public EthHelper(@Autowired Web3j web3) {
        this.web3 = web3;
    }

    public Log getLog(String trxHash) throws IOException {
        EthGetTransactionReceipt transactionReceipt = web3.ethGetTransactionReceipt(trxHash).send();
        return transactionReceipt.getResult().getLogs().stream().filter(logl -> logl.getTopics().size() == 4).findFirst().orElse(null);
    }

    public Double valueToDouble(BigInteger value) {
        return value.divide(BigInteger.valueOf(1000000000000000l)).doubleValue() / 1000;
    }

    public Transaction getTransaction(String trxHash) throws IOException {
        return web3.ethGetTransactionByHash(trxHash).send().getTransaction().get();
    }

    public Flowable<Transaction> getFlowable() {
        return web3.transactionFlowable();
    }


    public Optional<String> getTokenUri(String contract, Long tokenId) {
        try {
            org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                    "tokenURI",
                    Arrays.asList(new Uint256(tokenId)),
                    Arrays.asList(new TypeReference<Utf8String>() {
                    }));

            String encodedFunction = FunctionEncoder.encode(function);

            org.web3j.protocol.core.methods.response.EthCall response =
                    web3.ethCall(org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(null, contract, encodedFunction), DefaultBlockParameterName.LATEST)
                            .sendAsync().get();

            List<Type> someTypes = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters());

            return Optional.of(someTypes.get(0).toString());
        } catch (Exception e) {
            log.error("Error retrieving token URI, contract : {}, id : {}", contract, tokenId);
            return Optional.empty();
        }
    }
}
