package dev.mouradski.sgbnftbot.pattern.songbird;

import org.springframework.stereotype.Component;
import org.web3j.abi.TypeDecoder;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;

@Component
public class SparklesWSgbTokenDirectBuyPattern extends SparklesDirectBuyPattern {


    @Override
    protected String getTransactionFunction() {
        return "0x3b00ff7e";
    }

    @Override
    protected Double extracePrice(Transaction transaction) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        var value = ethHelper.getWeb3(getNetwork()).ethGetTransactionReceipt(transaction.getHash()).send().getResult().getLogs().stream()
                .filter(log -> log.getTopics().get(0).startsWith("0xddf252ad"))
                .filter(log -> log.getTopics().size() == 3)
                .map(log -> log.getData().replace("0x", ""))
                .map(data -> {
                    try {
                        return new BigInteger(TypeDecoder.instantiateType("uint256", data).getValue().toString()).divide(BigInteger.valueOf(1000000000000000l));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return BigInteger.ZERO;
                    }
                })
                .mapToDouble(BigInteger::doubleValue)
                .sum();


        return value / 2 / 1000 ;
    }
}
