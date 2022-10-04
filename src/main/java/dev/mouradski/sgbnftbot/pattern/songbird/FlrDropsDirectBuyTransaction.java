package dev.mouradski.sgbnftbot.pattern.songbird;

import dev.mouradski.sgbnftbot.model.Marketplace;
import dev.mouradski.sgbnftbot.model.Network;
import dev.mouradski.sgbnftbot.model.TransactionType;
import dev.mouradski.sgbnftbot.pattern.TransactionPattern;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;

@Component
public class FlrDropsDirectBuyTransaction extends TransactionPattern {

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
        return "0xe99a3f80";
    }

    @Override
    protected Marketplace getMarketplace() {
        return Marketplace.FlrDrops;
    }

    @Override
    protected String extractNftContract(Transaction transaction) throws IOException {
        Log log = ethHelper.getLog(transaction.getHash(), getNetwork());
        return log.getAddress();
    }

    @Override
    protected String extractBuyer(Transaction transaction) throws IOException {
        return transaction.getFrom();
    }


    @Override
    protected Long extractTokenId(Transaction transaction) throws IOException {
        Log log = ethHelper.getLog(transaction.getHash(), getNetwork());
        return Long.parseLong(log.getTopics().get(3).replace("0x", ""), 16);
    }

    @Override
    protected String getMarketplaceListingUrl(Transaction transaction) throws IOException {
        return "https://xfd.flr.finance/t/"+ extractNftContract(transaction) + "/" + extractTokenId(transaction);
    }

    @Override
    protected Double extracePrice(Transaction transaction) {
        return ethHelper.valueToDouble(transaction.getValue());
    }


    @Override
    public boolean matches(Transaction transaction, Network network) {
        return super.matches(transaction, network) && transaction.getInput().length() == 4170;
    }
}
