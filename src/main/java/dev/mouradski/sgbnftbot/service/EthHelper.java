package dev.mouradski.sgbnftbot.service;

import dev.mouradski.sgbnftbot.model.Network;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class EthHelper {

    private Web3j songbirdWeb3;

    private Web3j flareWeb3;

    public EthHelper(@Qualifier("songbirdWeb3") Web3j songbirdWeb3, @Qualifier("flareWeb3") Web3j flareWeb3) {
        this.songbirdWeb3 = songbirdWeb3;
        this.flareWeb3 = flareWeb3;
    }

    public Log getLog(String trxHash, Network network) throws IOException {
        var transactionReceipt = getWeb3(network).ethGetTransactionReceipt(trxHash).send();
        return transactionReceipt.getResult().getLogs().stream().filter(logl -> logl.getTopics().size() == 4).findFirst().orElse(null);
    }

    public Double valueToDouble(BigInteger value) {
        return value.divide(BigInteger.valueOf(1000000000000000l)).doubleValue() / 1000;
    }

    public Transaction getTransaction(String trxHash, Network network) throws IOException {
        return getWeb3(network).ethGetTransactionByHash(trxHash).send().getTransaction().get();
    }

    public Flowable<Transaction> getFlowable(Network network) {
        return getWeb3(network).transactionFlowable();
    }


    public Optional<String> getTokenUri(String contract, Long tokenId, Network network) {
        try {
            var function = new org.web3j.abi.datatypes.Function(
                    "tokenURI",
                    Arrays.asList(new Uint256(tokenId)),
                    Arrays.asList(new TypeReference<Utf8String>() {
                    }));

            var encodedFunction = FunctionEncoder.encode(function);

            var response =
                    getWeb3(network).ethCall(org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(null, contract, encodedFunction), DefaultBlockParameterName.LATEST)
                            .sendAsync().get();

            var someTypes = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters());

            return Optional.of(someTypes.get(0).toString());
        } catch (Exception e) {
            log.error("Error retrieving token URI, contract : {}, id : {}", contract, tokenId);
            return Optional.empty();
        }
    }

    public Web3j getWeb3(Network network) {
        if (Network.FLARE.equals(network)) {
            return flareWeb3;
        } else {
            return songbirdWeb3;
        }
    }

    public Double getNativeTokenUsdPrice(Network network) throws ExecutionException, InterruptedException {
        var ftsoContract = getFtsoContract(network, 9);

        var web3 = Network.FLARE.equals(network) ? flareWeb3 : songbirdWeb3;

        org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                "getCurrentPrice",
                Arrays.asList(),
                Collections.singletonList(new TypeReference<Uint256>() {
                }));

        var encodedFunction = FunctionEncoder.encode(function);

        org.web3j.protocol.core.methods.response.EthCall response =
                web3.ethCall(org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(null, ftsoContract, encodedFunction), DefaultBlockParameterName.LATEST)
                        .sendAsync().get();

        var someTypes = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());


        return ((Uint256) someTypes.get(0)).getValue().doubleValue() / 100000;
    }

    public String getFtsoContract(Network network, int index) throws ExecutionException, InterruptedException {

        var web3 = Network.FLARE.equals(network) ? flareWeb3 : songbirdWeb3;

        var ftsoManager = getFtsoManager(network);

        org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                "getFtsos",
                Arrays.asList(),
                Collections.singletonList(new TypeReference<DynamicArray<Address>>() {
                }));

        var encodedFunction = FunctionEncoder.encode(function);

        org.web3j.protocol.core.methods.response.EthCall response =
                web3.ethCall(org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(null, ftsoManager, encodedFunction), DefaultBlockParameterName.LATEST)
                        .sendAsync().get();

        var someTypes = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());

        return ((DynamicArray) ((List) someTypes).get(0)).getValue().get(index).toString();
    }

    private String getFtsoManager(Network network) throws ExecutionException, InterruptedException {
        var web3 = Network.FLARE.equals(network) ? flareWeb3 : songbirdWeb3;

        org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                "getFtsoManager",
                Arrays.asList(),
                Arrays.asList(new TypeReference<Address>() {
                }));

        var encodedFunction = FunctionEncoder.encode(function);

        org.web3j.protocol.core.methods.response.EthCall response =
                web3.ethCall(org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(null, "0x1000000000000000000000000000000000000003", encodedFunction),
                                DefaultBlockParameterName.LATEST)
                        .sendAsync().get();

        var someTypes = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());


        return someTypes.get(0).toString();
    }

}
