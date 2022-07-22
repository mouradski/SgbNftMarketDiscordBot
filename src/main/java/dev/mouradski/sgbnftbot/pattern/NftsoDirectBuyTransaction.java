package dev.mouradski.sgbnftbot.pattern;

import dev.mouradski.sgbnftbot.model.Marketplace;
import dev.mouradski.sgbnftbot.model.TransactionType;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class NftsoDirectBuyTransaction extends TransactionPattern {

    @Override
    protected TransactionType getTransactionType() {
        return TransactionType.BUY;
    }

    @Override
    protected List<String> getPatternContract() {
        return Arrays.asList("0x5cb9398ca62e941ef4d4aa5f7003f332c3b4b132");
    }

    @Override
    protected List<String> getTransactionFunction() {
        return Arrays.asList("0x1f6a8318");
    }

    @Override
    protected Marketplace getMarketplace() {
        return Marketplace.NFTSO;
    }

    @Override
    protected String extractNftContract(Transaction transaction) throws IOException {
        Log log = ethHelper.getLog(transaction.getHash());
        return log.getAddress();
    }

    @Override
    protected String extractBuyer(Transaction transaction) {
        return transaction.getFrom();
    }

    @Override
    protected Long extractTokenId(Transaction transaction) throws IOException {
        Log log = ethHelper.getLog(transaction.getHash());
        return Long.parseLong(log.getTopics().get(3).replace("0x", ""), 16);
    }

    @Override
    protected String getMarketplaceListingUrl(Transaction transaction) throws IOException {
        return "https://nftso.xyz/item-details/19/" + extractNftContract(transaction) + "/" + extractTokenId(transaction);
    }

    @Override
    protected Double extracePrice(Transaction transaction) {
        return ethHelper.valueToDouble(transaction.getValue());
    }
}
