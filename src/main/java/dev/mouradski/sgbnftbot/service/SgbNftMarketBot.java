package dev.mouradski.sgbnftbot.service;

import dev.mouradski.sgbnftbot.model.Meta;
import dev.mouradski.sgbnftbot.model.Network;
import dev.mouradski.sgbnftbot.model.SaleNotification;
import dev.mouradski.sgbnftbot.model.Subscription;
import dev.mouradski.sgbnftbot.pattern.TransactionPattern;
import lombok.extern.slf4j.Slf4j;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.Transaction;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@EnableScheduling
@Slf4j
public class SgbNftMarketBot {

    private SubscriptionService subscriptionService;

    private IpfsHelper ipfsHelper;

    private EthHelper ethHelper;

    private DiscordApi discordApi;

    private List<TransactionPattern> transactionPatterns;
    
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    private ExecutorService subscriptionsExecutor = Executors.newFixedThreadPool(3);
    private ExecutorService processExecutor = Executors.newFixedThreadPool(3);

    public SgbNftMarketBot(SubscriptionService subscriptionService, IpfsHelper ipfsHelper,
                           EthHelper ethHelper, List<TransactionPattern> transactionPatterns,
                           @Autowired(required = false) DiscordApi discordApi) {
        this.subscriptionService = subscriptionService;
        this.ipfsHelper = ipfsHelper;
        this.ethHelper = ethHelper;
        this.transactionPatterns = transactionPatterns;
        this.discordApi = discordApi;
    }

    private Set<String> contracts = new HashSet<>();


    @PostConstruct
    public void start() {
        subscriptionService.getAll().forEach(subscription -> contracts.add(subscription.getContract()));

        if (discordApi != null) {
            discordApi.addMessageCreateListener(event -> {
                if (event.getMessageContent().contains("!nftsales subscribe")) {
                    processSubscribeCommand(event);
                } else if (event.getMessageContent().contains("!nftsales unsubscribe")) {
                    processUnsubscribeCommand(event);
                } else if (event.getMessageContent().contains("!nftsales help")) {
                    processHelpCommand(event);
                } else if (event.getMessageContent().contains("!nftsales list")) {
                    processLisCommand(event);
                }
            });


            Stream.of(Network.SONGBIRD, Network.FLARE).forEach(network ->
                this.ethHelper.getFlowable(network).subscribe(transaction ->
                    processExecutor.execute(() ->
                        process(transaction, network)
                    )
                )
            );
        }
    }

    private void processSubscribeCommand(MessageCreateEvent event) {
        if (event.getMessageAuthor().isServerAdmin() && event.getServer().isPresent()) {

            String[] params = event.getMessageContent().split(" ");

            final String contract = params.length > 2 ? event.getMessageContent().split(" ")[2] : null;

            if (contract != null && !contract.trim().isEmpty() && contract.trim().length() == 42) {
                subscriptionsExecutor.execute(() -> {
                    try {
                        subscribeContract(event.getChannel(), contract.toLowerCase(), event.getServer().orElse(null), Network.SONGBIRD);
                    } catch (Exception e) {
                        try {
                            subscribeContract(event.getChannel(), contract.toLowerCase(), event.getServer().orElse(null), Network.FLARE);
                        } catch (Exception e2) {
                            log.error("Error subscribing contract : {}, channelId : {}", contract, event.getChannel().getIdAsString(), e2);
                        }
                    }
                });
            }
        }
    }

    private void processLisCommand(MessageCreateEvent event) {
        if (event.getMessageAuthor().isServerAdmin()) {
            subscriptionService.listAndSendSubscriptions(event.getChannel());
        }
    }

    private void processHelpCommand(MessageCreateEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Commandes :")
                .addField("Subscription command", "!nftsales subscribe CONTRACT_ADDRESS")
                .addField("Unsubscription command", "!nftsales unsubscribe CONTRACT_ADDRESS")
                .addField("List Subscriptions command", "!nftsales list")
                .setColor(Color.BLUE);

        event.getChannel().sendMessage(embed);
    }

    private void processUnsubscribeCommand(MessageCreateEvent event) {
        if (event.getMessageAuthor().isServerAdmin() && event.getServer().isPresent()) {
            String[] params = event.getMessageContent().split(" ");

            String contract = null;

            if (params.length > 2) {
                contract = event.getMessageContent().split(" ")[2];
            }

            if (contract != null && !contract.trim().isEmpty() && contract.trim().length() == 42) {
                try {
                    unsubscribe(event.getChannel(), contract.toLowerCase());

                } catch (Exception e) {
                    event.getChannel().sendMessage("Failed, please try again in a few seconds");
                }
            }
        }
    }

    public Optional<SaleNotification> process(String transactionHash, Network network) {
        try {
            return process(ethHelper.getTransaction(transactionHash, network), network);
        } catch (IOException e) {
            log.error("Error retreiving transaction {}", transactionHash, e);
            return Optional.empty();
        }
    }

    public Optional<SaleNotification> process(Transaction transaction, Network network) {
        try {
            Optional<TransactionPattern> matchingTransactionPattern = transactionPatterns.stream()
                    .filter(pattern -> pattern.matches(transaction, network))
                    .findFirst();

            if (matchingTransactionPattern.isPresent()) {
                SaleNotification saleNotification = matchingTransactionPattern.get().buildNotification(transaction);

                Set<Subscription> subscriptions = subscriptionService.getByContract(saleNotification.getContract().toLowerCase()).stream().collect(Collectors.toSet());

                saleNotification.setSubscriptions(subscriptions);

                notifySale(saleNotification, network);
                return Optional.of(saleNotification);
            } else {
                return Optional.empty();
            }

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void subscribeContract(TextChannel channel, String contract, Server server, Network network) throws IOException {

        if (subscriptionService.subscribeContract(contract, channel, server)) {
            return;
        }

        Optional<Meta> meta = ipfsHelper.retreiveMetaFromCollection(contract, network);


        subscriptionService.subscribeContract(contract, meta, channel, server);

        contracts.add(contract);
    }

    private void unsubscribe(TextChannel channel, String contract) {
        subscriptionService.unsubscribe(channel, contract);
    }

    private void notifySale(SaleNotification saleNotification, Network network) throws IOException {

        if (discordApi == null) {
            return;
        }

        if (saleNotification.getSubscriptions() == null || saleNotification.getSubscriptions().isEmpty()) {
            return;
        }

        String transactionTypeValue = null;

        switch (saleNotification.getTransactionType()) {
            case OFFER_ACCEPTED:
                transactionTypeValue = "Offer Accepted";
                break;
            case BUY:
            default:
                transactionTypeValue = "Buy";
        }


        String tokenName = saleNotification.getSubscriptions().stream().findFirst().get().getTokenName();

        Optional<String> metaUri = ethHelper.getTokenUri(saleNotification.getContract(), saleNotification.getTokenId(), network);
        String metaIpfsUri = null;

        if (metaUri.isPresent()) {
            metaIpfsUri = metaUri.get().replace("https://ipfs.io/ipfs/", "ipfs://");
        } else {
            return;
        }

        Optional<Meta> meta = ipfsHelper.getMeta(metaIpfsUri);

        if (!meta.isPresent()) {
            return;
        }

        Optional<byte[]> imageContent = ipfsHelper.get(meta.get().getImage());

	String token = Network.FLARE.equals(network) ? "FLR" : "SGB";

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(tokenName + " #" + saleNotification.getTokenId() + " has been sold !")
                //.addField("Buyer", saleNotification.getBuyer())
                .addInlineField("Token ID", saleNotification.getTokenId().toString())
                .addInlineField("Price", NUMBER_FORMAT.format(saleNotification.getPrice()) + " " + token)
                .addInlineField("Marketplace", saleNotification.getMarketplace().toString())
                .addInlineField("TransactionType", transactionTypeValue)
                .addInlineField("Network", saleNotification.getNetwork().toString())
                .setUrl(saleNotification.getMarketplaceListingUrl())
                .setColor(Color.BLUE);

        if (imageContent.isPresent()) {
            embed.setImage(imageContent.get());
        } else {
            embed.setImage(meta.get().getImage().replace("ipfs://", "https://ipfs.io/ipfs/"));
        }

        saleNotification.getSubscriptions().forEach(subscription -> {
            Optional<TextChannel> channel = discordApi.getTextChannelById(subscription.getChannelId());
            try {
                if (channel.isPresent()) {
                    channel.get().sendMessage(embed).join();
                }
            } catch (Exception e) {
                log.error("Unable to send message triggered from transaction {} to channel {}", saleNotification.getTransactionHash(), channel.get().getIdAsString(), e);
            }
        });
    }
}
