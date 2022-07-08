package dev.mouradski.sgbnftbot.service;

import dev.mouradski.sgbnftbot.model.*;
import dev.mouradski.sgbnftbot.pattern.TransactionPattern;
import dev.mouradski.sgbnftbot.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
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

    private SubscriptionRepository subscriptionRepository;

    private IpfsHelper ipfsService;

    private EthHelper ethHelper;

    private List<TransactionPattern> transactionPatterns;

    @Value("${app.production:false}")
    private boolean production;

    public SgbNftMarketBot(@Autowired SubscriptionRepository subscriptionRepository, @Autowired IpfsHelper ipfsService,
                           @Autowired EthHelper ethHelper, @Autowired List<TransactionPattern> transactionPatterns) {
        this.subscriptionRepository = subscriptionRepository;
        this.ipfsService = ipfsService;
        this.ethHelper = ethHelper;
        this.transactionPatterns = transactionPatterns;
    }

    private ExecutorService subscriptionsExecutor = Executors.newFixedThreadPool(3);

    private ExecutorService processExecutor = Executors.newFixedThreadPool(4);

    private DiscordApi discordApi;

    private Set<String> contracts = new HashSet<>();

    @Value("${discord.token}")
    private String token;


    @PostConstruct
    public void start() {

        subscriptionRepository.findAll().forEach(subscription -> {
            contracts.add(subscription.getContract());
        });

        if (production) {
            discordApi = new DiscordApiBuilder().setToken(token).setAllNonPrivilegedIntents()
                    .login().join();

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
                        e.printStackTrace();
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

    public Optional<SaleNotification> process(String transactionHash) throws IOException {
        SaleNotification saleNotification = process(ethHelper.getTransaction(transactionHash));

        if (saleNotification == null) {
            return Optional.empty();
        } else {
            return Optional.of(saleNotification);
        }

    }

    public SaleNotification process(Transaction transaction) {
        try {
            TransactionPattern transactionPattern = transactionPatterns.stream()
                    .filter(pattern -> pattern.matches(transaction))
                    .findFirst().orElse(null);

            SaleNotification saleNotification = transactionPattern.buildNotification(transaction);

            Set<Subscription> subscriptions = subscriptionRepository.findByContract(saleNotification.getContract()).stream().collect(Collectors.toSet());
            saleNotification.setSubscriptions(subscriptions);

            notifySale(saleNotification);
            return saleNotification;
        } catch (Exception e) {
            return null;
        }
    }

     public void subscribeContract(String channel, String contract, Server server) throws IOException {
         subscribeContract(discordApi.getTextChannelById(channel).get(), contract, server);
     }

    public void subscribeContract(TextChannel channel, String contract, Server server) throws IOException {

        SubscriptionId subscriptionId = SubscriptionId.builder().channelId(channel.getIdAsString()).contract(contract).build();

        if (subscriptionRepository.existsById(subscriptionId)) {

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Already subscribed to " + subscriptionRepository.findById(subscriptionId).get().getTokenName())
                    .addField("Command to unsubscribe", "!nftsales unsubscribe " + contract)
                    .setColor(Color.BLUE);
            channel.sendMessage(embed);

            return;
        }

        List<Subscription> subscriptionList = subscriptionRepository.findByContract(contract);

        if (!subscriptionList.isEmpty()) {

            Subscription firstSubscription = subscriptionList.get(0);

            Subscription newSubscription = Subscription.builder()
                    .imageBaseUrl(firstSubscription.getImageBaseUrl())
                    .imageExtension(firstSubscription.getImageExtension())
                    .contract(contract)
                    .serverName(server == null ? null : server.getName())
                    .channelId(channel.getIdAsString())
                    .tokenName(firstSubscription.getTokenName()).build();

            subscriptionRepository.save(newSubscription);

            EmbedBuilder nembed = new EmbedBuilder()
                    .setTitle("Subscription successfully !")
                    .addField("Project Name", newSubscription.getTokenName())
                    .addField("Command to unsubscribe", "!nftsales unsubscribe " + contract)
                    .setColor(Color.BLUE);

            channel.sendMessage(nembed);

            return;
        }

        Meta meta = retreiveMetaFromCollection(contract);
        String nftName = meta.getName();

        if (!nftName.isEmpty()) {
            nftName = meta.getName().replaceAll("#[0-9]+", "").replace("-", "").trim();
        }

        Subscription subscription = buildSubscription(contract, nftName, server, channel);

        subscriptionRepository.save(subscription);
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

    private Subscription buildSubscription(String contract, String nftName, Server server, TextChannel channel) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Subscription successfully !")
                .addField("Project Name", nftName)
                .addField("Command to unsubscribe", "!nftsales unsubscribe " + contract)
                .setColor(Color.BLUE);

        Subscription subscription = Subscription.builder()
                .channelId(channel.getIdAsString()).contract(contract)
                .imageExtension(null).imageBaseUrl(null)
                .serverName(server == null ? null : server.getName()).tokenName(nftName).build();


        channel.sendMessage(embed);

        return subscription;
    }


    private void unsubscribe(TextChannel channel, String contract) {

        Subscription subscription = subscriptionRepository.findById(SubscriptionId.builder().channelId(channel.getIdAsString()).contract(contract.toLowerCase()).build()).orElse(null);

        if (subscription != null) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Unsubscribed successfully !")
                    .addField("Project Name", subscription.getTokenName())
                    .setColor(Color.BLUE);

            channel.sendMessage(embed);
        }

        subscriptionRepository.deleteById(SubscriptionId.builder().channelId(channel.getIdAsString()).contract(contract).build());
    }

    private void notifySale(SaleNotification saleNotification) throws IOException {

        if (!production) {
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

        Subscription subscription = saleNotification.getSubscriptions().stream().findFirst().get();

        String tokenName = subscription.getTokenName();

        String metaIpfsUri = ethHelper.getTokenUri(saleNotification.getContract(), saleNotification.getTokenId()).replace("https://ipfs.io/ipfs/", "ipfs://");

        Meta meta = ipfsService.getMeta(metaIpfsUri);

        byte[] imageContent = ipfsService.get(meta.getImage());

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(tokenName + " #" + saleNotification.getTokenId() + " has been sold !")
                .addField(TransactionType.OFFER_ACCEPTED.equals(saleNotification.getTransactionType()) ? "Seller" : "Buyer", saleNotification.getTrigger())
                .addInlineField("Token ID", saleNotification.getTokenId().toString())
                .addInlineField("Price", saleNotification.getPrice() + " SGB")
                .addInlineField("Marketplace", saleNotification.getMarketplaceAVirer().split("_")[0])
                .addInlineField("TransactionType", transactionTypeValue)
                .setUrl(saleNotification.getMarketplaceListingUrl())
                .setColor(Color.BLUE);

        if (imageContent != null) {
            embed.setImage(imageContent);
        } else {
            embed.setImage(meta.getImage().replace("ipfs://", "https://ipfs.io/ipfs/"));
        }

        saleNotification.getSubscriptions().forEach(sub -> {
            TextChannel channel = discordApi.getTextChannelById(sub.getChannelId()).orElse(null);
            if (channel != null) {
                channel.sendMessage(embed);
            }
        });
    }
    
}