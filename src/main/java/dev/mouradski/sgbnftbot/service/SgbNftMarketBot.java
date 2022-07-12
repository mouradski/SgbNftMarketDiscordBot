package dev.mouradski.sgbnftbot.service;

import dev.mouradski.sgbnftbot.model.Meta;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.Transaction;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


@Component
@EnableScheduling
@Slf4j
public class SgbNftMarketBot {

    private SubscriptionService subscriptionService;

    private SaleNotificationService saleNotificationService;

    private IpfsHelper ipfsService;

    private EthHelper ethHelper;

    private DiscordApi discordApi;

    private List<TransactionPattern> transactionPatterns;

    private ExecutorService subscriptionsExecutor = Executors.newFixedThreadPool(3);
    private ExecutorService processExecutor = Executors.newFixedThreadPool(3);

    public SgbNftMarketBot(@Autowired SubscriptionService subscriptionService, @Autowired IpfsHelper ipfsService,
                           @Autowired EthHelper ethHelper, @Autowired List<TransactionPattern> transactionPatterns,
                           @Autowired SaleNotificationService saleNotificationService, @Autowired(required = false) DiscordApi discordApi) {
        this.subscriptionService = subscriptionService;
        this.ipfsService = ipfsService;
        this.ethHelper = ethHelper;
        this.transactionPatterns = transactionPatterns;
        this.saleNotificationService = saleNotificationService;
        this.discordApi = discordApi;
    }

    private Set<String> contracts = new HashSet<>();


    @PostConstruct
    public void start() {
        subscriptionService.getAll().forEach(subscription -> {
            contracts.add(subscription.getContract());
        });

        if (discordApi != null) {
            discordApi.addMessageCreateListener(event -> {
                if (event.getMessageContent().contains("!nftsales subscribe")) {
                    processSubscribeCommand(event);
                } else if (event.getMessageContent().contains("!nftsales unsubscribe")) {
                    processUnsubscribeCommand(event);
                } else if (event.getMessageContent().contains("!nftsales help")) {
                    processHelpCommand(event);
                }
            });

            this.ethHelper.getFlowable().subscribe(transaction -> {
                processExecutor.execute(() -> {
                    process(transaction);
                });
            });
        }
    }

    private void processSubscribeCommand(MessageCreateEvent event) {
        if (event.getMessageAuthor().isServerAdmin() && event.getServer().isPresent()) {

            String[] params = event.getMessageContent().split(" ");

            final String contract = params.length > 2 ? event.getMessageContent().split(" ")[2] : null;

            if (contract != null && !contract.trim().isEmpty() && contract.trim().length() == 42) {
                subscriptionsExecutor.execute(() -> {
                    try {
                        subscribeContract(event.getChannel(), contract.toLowerCase(), event.getServer().orElse(null));
                    } catch (Exception e) {
                        log.error("Error subscribing contract : {}, channelId : {}", contract, event.getChannel().getIdAsString(), e);
                    }
                });
            }
        }
    }

    private void processHelpCommand(MessageCreateEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Commandes :")
                .addField("Subscription command", "!nftsales subscribe CONTRACT_ADDRESS")
                .addField("Unsubscription command", "!nftsales unsubscribe CONTRACT_ADDRESS")
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

    public Optional<SaleNotification> process(String transactionHash) {
        try {
            return process(ethHelper.getTransaction(transactionHash));
        } catch (IOException e) {
            log.error("Error retreiving transaction {}", transactionHash, e);
            return Optional.empty();
        }
    }

    public Optional<SaleNotification> process(Transaction transaction) {
        try {
            Optional<TransactionPattern> matchingTransactionPattern = transactionPatterns.stream()
                    .filter(pattern -> pattern.matches(transaction))
                    .findFirst();

            if (matchingTransactionPattern.isPresent()) {
                SaleNotification saleNotification = matchingTransactionPattern.get().buildNotification(transaction);

                Set<Subscription> subscriptions = subscriptionService.getByContract(saleNotification.getContract().toLowerCase()).stream().collect(Collectors.toSet());

                saleNotification.setSubscriptions(subscriptions);

                notifySale(saleNotification);
                return Optional.of(saleNotification);
            } else {
                return Optional.empty();
            }

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void subscribeContract(String channel, String contract, Server server) throws IOException {
        subscribeContract(discordApi.getTextChannelById(channel).get(), contract, server);
    }

    public void subscribeContract(TextChannel channel, String contract, Server server) throws IOException {

        if (subscriptionService.subscribeContract(contract, channel, server)) {
            return;
        }

        Meta meta = retreiveMetaFromCollection(contract);
        String nftName = meta.getName();

        if (!nftName.isEmpty()) {
            nftName = meta.getName().replaceAll("#[0-9]+", "").replace("-", "").trim();
        }

        subscriptionService.subscribeContract(contract, nftName, channel, server);

        contracts.add(contract);
    }


    private Meta retreiveMetaFromCollection(String contract) throws IOException {

        int i = 0;
        String metaIpfsUri = null;

        while (i++ < 100 && metaIpfsUri == null) {
            metaIpfsUri = ethHelper.getTokenUri(contract, Long.valueOf(i));
        }

        if (metaIpfsUri == null) {
            return null;
        }

        return ipfsService.getMeta(metaIpfsUri);
    }

    private void unsubscribe(TextChannel channel, String contract) {
        subscriptionService.unsubscribe(channel, contract);
    }

    private void notifySale(SaleNotification saleNotification) throws IOException {

        if (discordApi == null) {
            return;
        }

        if (saleNotification.getSubscriptions() == null || saleNotification.getSubscriptions().isEmpty()) {
            return;
        }

        String tokenName = saleNotification.getSubscriptions().stream().findFirst().get().getTokenName();

        String metaIpfsUri = ethHelper.getTokenUri(saleNotification.getContract(), saleNotification.getTokenId()).replace("https://ipfs.io/ipfs/", "ipfs://");

        Meta meta = ipfsService.getMeta(metaIpfsUri);

        byte[] imageContent = ipfsService.get(meta.getImage());

        saleNotification.setImageContent(imageContent);
        saleNotification.setImageUrl(meta.getImage());

        saleNotification.getSubscriptions().forEach(subscription -> {
            TextChannel channel = discordApi.getTextChannelById(subscription.getChannelId()).orElse(null);
            saleNotificationService.sendSaleNotificationLog(channel, subscription, saleNotification, tokenName);
        });
    }


    @Scheduled(fixedDelay = 43200000)
    public void purgeLogs() {
        saleNotificationService.purgeLogs();
    }

    @Scheduled(fixedDelay = 60000)
    public void replyFailedNotifications() {
        saleNotificationService.getTransactionsToReplay()
                .forEach(this::process);
    }

}
