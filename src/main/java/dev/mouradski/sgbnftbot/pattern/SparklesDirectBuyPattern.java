package dev.mouradski.sgbnftbot.pattern;

import dev.mouradski.sgbnftbot.model.Marketplace;
import dev.mouradski.sgbnftbot.model.TransactionType;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@Component
public class SparklesDirectBuyPattern extends TransactionPattern {

    @Override
    protected TransactionType getTransactionType() {
        return TransactionType.BUY;
    }

    @Override
    protected String getPatternContract() {
        return "0xc1c02a72de998a4659b8d430e741446d1f5e268f";
    }

    @Override
    protected String getTransactionFunction() {
        return "0x18418ee7";
    }

    @Override
    protected Marketplace getMarketplace() {
        return Marketplace.SparklesNFT;
    }

    @Override
    protected String extractNftContract(Transaction transaction) throws IOException {
        Log log = ethHelper.getLog(transaction.getHash());
        return log.getTopics().get(1).replace("0x000000000000000000000000", "0x");
    }

    @Override
    protected String extractBuyer(Transaction transaction) throws IOException {
        return transaction.getFrom();
    }


    @Override
    protected Long extractTokenId(Transaction transaction) throws IOException {
        Log log = ethHelper.getLog(transaction.getHash());
        return Long.parseLong(log.getTopics().get(2).replace("0x", ""), 16);
    }

    @Override
    protected String getMarketplaceListingUrl(Transaction transaction) throws IOException {
        return "https://www.sparklesnft.com/item/" + extractNftContract(transaction) + "_" + extractTokenId(transaction);
    }

    @Override
    protected Double extracePrice(Transaction transaction) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return ethHelper.valueToDouble(transaction.getValue());
    }
}
