package dev.mouradski.sgbnftbot.pattern.songbird;

import dev.mouradski.sgbnftbot.model.Marketplace;
import dev.mouradski.sgbnftbot.model.Network;
import dev.mouradski.sgbnftbot.model.TransactionType;
import dev.mouradski.sgbnftbot.pattern.TransactionPattern;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@Component
public class SparklesDirectBuyPattern extends TransactionPattern {

    @Override
    protected Network getNetwork() {
        return Network.SONGBIRD;
    }

    @Override
    protected TransactionType getTransactionType() {
        return TransactionType.BUY;
    }

    @Override
    protected String getTransactionFunction() {
        return "0x0b260134";
    }

    @Override
    protected Marketplace getMarketplace() {
        return Marketplace.SparklesNFT;
    }

    @Override
    protected String extractNftContract(Transaction transaction) throws IOException {
        Log log = ethHelper.getLog(transaction.getHash(), getNetwork());
        return log.getTopics().get(1).replace("0x000000000000000000000000", "0x");
    }

    @Override
    protected String extractBuyer(Transaction transaction) throws IOException {
        return transaction.getFrom();
    }


    @Override
    protected Long extractTokenId(Transaction transaction) throws IOException {
        Log log = ethHelper.getLog(transaction.getHash(), getNetwork());
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
