package dev.mouradski.sgbnftbot.pattern;

import dev.mouradski.sgbnftbot.model.Marketplace;
import dev.mouradski.sgbnftbot.model.TransactionType;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;

@Component
public class FlrDropsDirectBuyTransaction extends TransactionPattern {

    @Override
    protected TransactionType getTransactionType() {
        return TransactionType.BUY;
    }

    @Override
    protected String getTransactionFunction() {
        return "0xe99a3f80";
    }

    @Override
    protected Marketplace getMarketplace() {
        return Marketplace.FlrDrops;
    }

    @Override
    protected String extractNftContract(Transaction transaction) throws IOException {
        Log log = ethHelper.getLog(transaction.getHash());
        return log.getAddress();
    }

    @Override
    protected String extractBuyer(Transaction transaction) throws IOException {
        return transaction.getFrom();
    }


    @Override
    protected Long extractTokenId(Transaction transaction) throws IOException {
        Log log = ethHelper.getLog(transaction.getHash());
        return Long.parseLong(log.getTopics().get(3).replace("0x", ""), 16);
    }

    @Override
    protected String getMarketplaceListingUrl(Transaction transaction) throws IOException {
        return "https://xfd.flr.finance/t/"+ extractNftContract(transaction) + "/" + extractTokenId(transaction);
    }

    @Override
    protected Double extracePrice(Transaction transaction) throws IOException {
        return ethHelper.valueToDouble(transaction.getValue());
    }


    @Override
    public boolean matches(Transaction transaction) {
        return super.matches(transaction) && transaction.getInput().length() == 4170;
    }
}
