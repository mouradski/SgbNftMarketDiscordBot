package dev.mouradski.sgbnftbot.pattern;

import dev.mouradski.sgbnftbot.model.Marketplace;
import dev.mouradski.sgbnftbot.model.Network;
import dev.mouradski.sgbnftbot.model.SaleNotification;
import dev.mouradski.sgbnftbot.model.TransactionType;
import dev.mouradski.sgbnftbot.service.EthHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@Slf4j
public abstract class TransactionPattern {

    @Autowired
    protected EthHelper ethHelper;

    protected abstract Network getNetwork();

    protected abstract TransactionType getTransactionType();

    protected abstract String getTransactionFunction();

    protected abstract Marketplace getMarketplace();

    protected abstract String extractNftContract(Transaction transaction) throws IOException;

    protected abstract String extractBuyer(Transaction transaction) throws IOException;

    protected abstract Long extractTokenId(Transaction transaction) throws IOException;

    protected abstract String getMarketplaceListingUrl(Transaction transaction) throws IOException;

    protected abstract Double extracePrice(Transaction transaction) throws IOException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;

    public SaleNotification buildNotification(Transaction transaction) throws IOException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        var buyer = extractBuyer(transaction);
        var tokenId = extractTokenId(transaction);
        var price = extracePrice(transaction);
        var transactionType = getTransactionType();
        var nftContract = extractNftContract(transaction).toLowerCase();
        var marketplaceListingUrl = getMarketplaceListingUrl(transaction);

        return SaleNotification.builder().contract(nftContract)
                .buyer(buyer)
                .transactionType(transactionType)
                .marketplace(getMarketplace())
                .price(price)
                .tokenId(tokenId)
                .marketplaceListingUrl(marketplaceListingUrl)
                .transactionHash(transaction.getHash())
                .network(this.getNetwork())
                .build();
    }


    public boolean matches(Transaction transaction, Network network) {

        if (!getNetwork().equals(network)) {
            return false;
        }

        try {
            var function = transaction.getInput().substring(0, 10);

            return function.equalsIgnoreCase(getTransactionFunction()) &&
                    transaction.getInput() != null && transaction.getInput().length() > 10;
        } catch (Exception e) {
            return false;
        }
    }
}
