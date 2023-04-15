package dev.mouradski.sgbnftbot.pattern.songbird;

import dev.mouradski.sgbnftbot.model.Marketplace;
import dev.mouradski.sgbnftbot.model.Network;
import dev.mouradski.sgbnftbot.model.TransactionType;
import dev.mouradski.sgbnftbot.pattern.TransactionPattern;
import org.springframework.stereotype.Component;
import org.web3j.abi.TypeDecoder;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;
import java.math.BigInteger;

@Component
public class FlrDropsOfferAcceptedPattern extends TransactionPattern {

    @Override
    protected Network getNetwork() {
        return Network.SONGBIRD;
    }

    @Override
    protected TransactionType getTransactionType() {
        return TransactionType.OFFER_ACCEPTED;
    }

    @Override
    protected String getTransactionFunction() {
        return "0xe99a3f80";
    }

    @Override
    protected String extractNftContract(Transaction transaction) throws IOException {
        Log log = ethHelper.getLog(transaction.getHash(), getNetwork());
        return log.getAddress();
    }

    @Override
    protected String extractBuyer(Transaction transaction) throws IOException {
        return ethHelper.getWeb3(getNetwork()).ethGetTransactionReceipt(transaction.getHash()).send().getResult().getLogs().stream()
                .filter(log -> log.getTopics().get(0).startsWith("0xddf252ad"))
                .filter(log -> log.getTopics().size() == 3)
                .map(log -> log.getTopics().get(1).replace("0x000000000000000000000000", "0x"))
                .findFirst().get();
    }

    @Override
    protected Long extractTokenId(Transaction transaction) throws IOException {
        Log log = ethHelper.getLog(transaction.getHash(), getNetwork());
        return Long.parseLong(log.getTopics().get(3).replace("0x", ""), 16);
    }

    @Override
    protected Double extracePrice(Transaction transaction) throws IOException {
        Double value = ethHelper.getWeb3(getNetwork()).ethGetTransactionReceipt(transaction.getHash()).send().getResult().getLogs().stream()
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


        return value.doubleValue() / 1000 ;
    }


    @Override
    protected Marketplace getMarketplace() {
        return Marketplace.FlrDrops;
    }

    @Override
    protected String getMarketplaceListingUrl(Transaction transaction) throws IOException {
        return "https://xfd.flr.finance/t/"+ extractNftContract(transaction) + "/" + extractTokenId(transaction);
    }

    @Override
    public boolean matches(Transaction transaction, Network network) {
        return super.matches(transaction, network) && transaction.getInput().length() == 4298;
    }
}
