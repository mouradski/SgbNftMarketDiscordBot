package dev.mouradski.sgbnftbot.pattern;

import dev.mouradski.sgbnftbot.model.Marketplace;
import dev.mouradski.sgbnftbot.model.TransactionType;
import org.springframework.stereotype.Component;
import org.web3j.abi.TypeDecoder;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;
import java.math.BigInteger;

@Component
public class FlrDropsOfferAcceptedPattern extends TransactionPattern {

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
        Log log = ethHelper.getLog(transaction.getHash());
        return log.getAddress();
    }

    @Override
    protected String extractBuyer(Transaction transaction) throws IOException {
        return web3.ethGetTransactionReceipt(transaction.getHash()).send().getResult().getLogs().stream()
                .filter(log -> log.getTopics().get(0).startsWith("0xddf252ad"))
                .filter(log -> log.getTopics().size() == 3)
                .map(log -> log.getTopics().get(1).replace("0x000000000000000000000000", "0x"))
                .findFirst().get();
    }

    @Override
    protected Long extractTokenId(Transaction transaction) throws IOException {
        Log log = ethHelper.getLog(transaction.getHash());
        return Long.parseLong(log.getTopics().get(3).replace("0x", ""), 16);
    }

    @Override
    protected Double extracePrice(Transaction transaction) throws IOException {
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


    @Override
    protected Marketplace getMarketplace() {
        return Marketplace.FlrDrops;
    }

    @Override
    protected String getMarketplaceListingUrl(Transaction transaction) throws IOException {
        return "https://xfd.flr.finance/t/"+ extractNftContract(transaction) + "/" + extractTokenId(transaction);
    }

    @Override
    public boolean matches(Transaction transaction) {
        return super.matches(transaction) && transaction.getInput().length() == 4298;
    }
}
