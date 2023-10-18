package dev.mouradski.sgbnftbot.service;

import dev.mouradski.sgbnftbot.model.Meta;
import dev.mouradski.sgbnftbot.model.Subscription;
import dev.mouradski.sgbnftbot.model.SubscriptionId;
import dev.mouradski.sgbnftbot.repository.SubscriptionRepository;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    private SubscriptionRepository subscriptionRepository;


    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }


    @Transactional(readOnly = true)
    public List<Subscription> getAll() {
        return subscriptionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> get(String contract, String channelId) {
        SubscriptionId subscriptionId = SubscriptionId.builder().channelId(channelId).contract(contract).build();
        return subscriptionRepository.findById(subscriptionId);
    }

    @Transactional(readOnly = true)
    public List<Subscription> getByContract(String contract) {
        return subscriptionRepository.findByContract(contract);
    }



    @Transactional
    public void subscribeContract(String contract, Optional<Meta> meta, TextChannel channel, Server server) {

        if (meta.isEmpty()) {
            channel.sendMessage(new EmbedBuilder().setTitle("Error retrieving contract metadata, retry later !").setColor(Color.RED));
        }

        var nftName = meta.get().getName().replaceAll("#\\d+", "").replace("-", "").trim();

        var embed = new EmbedBuilder()
                .setTitle("Subscription successfully !")
                .addField("Project Name", nftName)
                .addField("Command to unsubscribe", "!nftsales unsubscribe " + contract)
                .setColor(Color.BLUE);

        var subscription = Subscription.builder()
                .channelId(channel.getIdAsString()).contract(contract)
                .serverName(server == null ? null : server.getName()).tokenName(nftName).build();


        channel.sendMessage(embed);

        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void unsubscribe(TextChannel channel, String contract) {
        var subscription = subscriptionRepository.findById(SubscriptionId.builder().channelId(channel.getIdAsString()).contract(contract.toLowerCase()).build()).orElse(null);

        if (subscription != null) {
            var embed = new EmbedBuilder()
                    .setTitle("Unsubscribed successfully !")
                    .addField("Project Name", subscription.getTokenName())
                    .setColor(Color.BLUE);

            channel.sendMessage(embed);
        }

        subscriptionRepository.deleteById(SubscriptionId.builder().channelId(channel.getIdAsString()).contract(contract).build());
    }


    @Transactional
    public boolean subscribeContract(String contract, TextChannel channel, Server server) {
        var subscriptionId = SubscriptionId.builder().channelId(channel.getIdAsString()).contract(contract).build();

        var subscription = subscriptionRepository.findById(subscriptionId);


        if (subscription.isPresent()) {

            var embed = new EmbedBuilder()
                    .setTitle("Already subscribed to " + subscription.get().getTokenName())
                    .addField("Command to unsubscribe", "!nftsales unsubscribe " + contract)
                    .setColor(Color.BLUE);
            channel.sendMessage(embed);

            return true;
        }

        var subscriptionList = subscriptionRepository.findByContract(contract);

        if (!subscriptionList.isEmpty()) {

            var firstSubscription = subscriptionList.get(0);

            var newSubscription = Subscription.builder()
                    .contract(contract)
                    .serverName(server == null ? null : server.getName())
                    .channelId(channel.getIdAsString())
                    .tokenName(firstSubscription.getTokenName()).build();

            subscriptionRepository.save(newSubscription);

            var nembed = new EmbedBuilder()
                    .setTitle("Subscription successfully !")
                    .addField("Project Name", newSubscription.getTokenName())
                    .addField("Command to unsubscribe", "!nftsales unsubscribe " + contract)
                    .setColor(Color.BLUE);

            channel.sendMessage(nembed);

            return true;
        }

        return false;
    }

    @Transactional(readOnly = true)
    public void listAndSendSubscriptions(TextChannel channel) {
        var subscriptions = subscriptionRepository.findByChannelId(channel.getIdAsString()).stream()
                .map(subscription -> subscription.getTokenName() + "  :  " + subscription.getContract())
                .collect(Collectors.toSet());

        int count = 0;

        var sb = new StringBuilder();

        for (String subscriptionLine : subscriptions) {
            sb.append(subscriptionLine).append("\n");

            if (count == 7) {
                count = 0;
                channel.sendMessage(sb.toString());
                sb = new StringBuilder();
            }

            count++;
        }

        if (sb.length() > 0) {
            channel.sendMessage(sb.toString());
        }
    }
}
