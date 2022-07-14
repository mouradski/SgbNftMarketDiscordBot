package dev.mouradski.sgbnftbot.pattern;

import dev.mouradski.sgbnftbot.model.TransactionType;
import org.springframework.stereotype.Component;
import org.web3j.abi.TypeDecoder;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Comparator;

@Component
public class SparklesOfferAcceptedPattern extends SparklesDirectBuyPattern {

    @Override
    protected TransactionType getTransactionType() {
        return TransactionType.OFFER_ACCEPTED;
    }

    @Override
    protected String getPatternContract() {
        return "0x42d8eb81d64d29b754acc8185a2c10a51fc7200d";
    }

    @Override
    protected String getTransactionFunction() {
        return "0xc815729d";
    }

    @Override
    protected String extractNftContract(Transaction transaction) throws IOException {
        Log log = ethHelper.getLog(transaction.getHash());
        return log.getAddress();
    }

    @Override
    protected String extractBuyer(Transaction transaction) {
        return null;
    }

    @Override
    protected String extractSeller(Transaction transaction) {
        return transaction.getFrom();
    }

    @Override
    protected Long extractTokenId(Transaction transaction) throws IOException {
        Log log = ethHelper.getLog(transaction.getHash());
        return Long.parseLong(log.getTopics().get(3).replace("0x", ""), 16);
    }

    @Override
    protected Double extracePrice(Transaction transaction) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Double value = web3.ethGetTransactionReceipt(transaction.getHash()).send().getResult().getLogs().stream()
                .filter(log -> log.getTopics().get(0).startsWith("0xddf252ad"))
                .filter(log -> log.getTopics().size() == 3)
                .map(log -> log.getData().replace("0x", ""))
                .map(data -> {
                    try {
                        return new BigInteger(TypeDecoder.instantiateType("uint256", data).getValue().toString()).divide(new BigInteger("1000000000000000"));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return new BigInteger("0");
                    }
                })
                .mapToDouble(BigInteger::doubleValue)
                .sum();


        return value.doubleValue() / 1000 ;
    }
}
