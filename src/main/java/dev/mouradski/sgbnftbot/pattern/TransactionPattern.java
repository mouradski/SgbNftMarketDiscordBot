package dev.mouradski.sgbnftbot.pattern;

import dev.mouradski.sgbnftbot.model.Marketplace;
import dev.mouradski.sgbnftbot.model.SaleNotification;
import dev.mouradski.sgbnftbot.model.TransactionType;
import dev.mouradski.sgbnftbot.service.EthHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@Slf4j
public abstract class TransactionPattern {

    @Autowired
    protected EthHelper ethHelper;

    @Autowired
    protected Web3j web3;

    protected abstract TransactionType getTransactionType();

    protected abstract String getPatternContract();

    protected abstract String getTransactionFunction();

    protected abstract Marketplace getMarketplace();

    protected abstract String extractNftContract(Transaction transaction) throws IOException;

    protected abstract String extractBuyer(Transaction transaction);

    protected abstract String extractSeller(Transaction transaction);

    protected abstract Long extractTokenId(Transaction transaction) throws IOException;

    protected abstract String getMarketplaceListingUrl(Transaction transaction) throws IOException;

    protected abstract Double extracePrice(Transaction transaction) throws IOException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;

    public SaleNotification buildNotification(Transaction transaction) throws IOException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        String buyer = extractBuyer(transaction);
        String seller = extractSeller(transaction);
        Long tokenId = extractTokenId(transaction);
        Double price = extracePrice(transaction);
        TransactionType transactionType = getTransactionType();
        String nftContract = extractNftContract(transaction);
        String marketplaceListingUrl = getMarketplaceListingUrl(transaction);

        String trigger = buyer != null ? buyer : seller;

        return SaleNotification.builder().contract(nftContract).buyer(buyer).seller(seller)
                .transactionType(transactionType)
                .marketplace(getMarketplace())
                .price(price.doubleValue()).tokenId(tokenId)
                .marketplaceListingUrl(marketplaceListingUrl)
                .trigger(trigger)
                .build();
    }


    public boolean matches(Transaction transaction) {
        String function = transaction.getInput().substring(0, 10);
        String marketplaceContract = transaction.getTo().toLowerCase();

        return function.equalsIgnoreCase(getTransactionFunction())
                && marketplaceContract.equalsIgnoreCase(getPatternContract()) &&
                transaction.getInput() != null & transaction.getInput().length() > 10;
    }
}
