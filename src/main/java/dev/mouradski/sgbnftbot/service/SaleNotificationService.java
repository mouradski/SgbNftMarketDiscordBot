package dev.mouradski.sgbnftbot.service;

import dev.mouradski.sgbnftbot.model.SaleNotification;
import dev.mouradski.sgbnftbot.model.SaleNotificationLog;
import dev.mouradski.sgbnftbot.model.Subscription;
import dev.mouradski.sgbnftbot.model.TransactionType;
import dev.mouradski.sgbnftbot.repository.SaleNotificationLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SaleNotificationService {

    private SaleNotificationLogRepository saleNotificationLogRepository;

    public SaleNotificationService(@Autowired SaleNotificationLogRepository saleNotificationLogRepository) {
        this.saleNotificationLogRepository = saleNotificationLogRepository;
    }

    @Transactional
    public void sendSaleNotificationLog(TextChannel channel, Subscription subscription, SaleNotification saleNotification, String tokenName) {

        if (channel == null) {
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


        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(tokenName + " #" + saleNotification.getTokenId() + " has been sold !")
                .addField(TransactionType.OFFER_ACCEPTED.equals(saleNotification.getTransactionType()) ? "Seller" : "Buyer", saleNotification.getTrigger())
                .addInlineField("Token ID", saleNotification.getTokenId().toString())
                .addInlineField("Price", saleNotification.getPrice() + " SGB")
                .addInlineField("Marketplace", saleNotification.getMarketplace().toString())
                .addInlineField("TransactionType", transactionTypeValue)
                .setUrl(saleNotification.getMarketplaceListingUrl())
                .setColor(Color.BLUE);

        if (saleNotification.getImageContent() != null) {
            embed.setImage(saleNotification.getImageContent());
        } else {
            embed.setImage(saleNotification.getImageUrl().replace("ipfs://", "https://ipfs.io/ipfs/"));
        }


        List<SaleNotificationLog> saleNotificationLogs = saleNotificationLogRepository
                .findByTransactionHashAndChannelIdAndFailedIsTrue(saleNotification.getTransactionHash(), channel.getIdAsString());

        SaleNotificationLog saleNotificationLog = null;

        if (saleNotificationLogs.size() > 0) {
            saleNotificationLog = saleNotificationLogs.get(0);
        }

        if (saleNotificationLog != null) {
            saleNotificationLog.setRetry(saleNotificationLog.getRetry() > 0 ? saleNotificationLog.getRetry() - 1 : 0);
            saleNotificationLog.setDate(OffsetDateTime.now());
            saleNotificationLogRepository.save(saleNotificationLog);
        }

        try {
            channel.sendMessage(embed).join();

            if (saleNotificationLog == null) {
                persistNewSaleNotificationLog(saleNotification, subscription, channel, false);
            }
            saleNotificationLogRepository.stopRetry(saleNotification.getTransactionHash(), channel.getIdAsString());
        } catch (Exception e) {
            log.error("Unable to send message triggered from transaction {} to channel {}", saleNotification.getTransactionHash(), channel.getIdAsString());
            persistNewSaleNotificationLog(saleNotification, subscription, channel, true);
        }
    }

    @Transactional
    public void purgeLogs() {
        saleNotificationLogRepository.deleteByDateBefore(OffsetDateTime.now().minusDays(10));
    }

    @Transactional(readOnly = true)
    public Set<String> getTransactionsToReplay() {
        return saleNotificationLogRepository.findByFailedIsTrueAndRetryGreaterThan(0).stream()
                .map(SaleNotificationLog::getTransactionHash).collect(Collectors.toSet());
    }

    private void persistNewSaleNotificationLog(SaleNotification saleNotification, Subscription subscription, TextChannel channel, boolean error) {
        saleNotificationLogRepository.save(SaleNotificationLog.builder().transactionHash(saleNotification.getTransactionHash())
                .channelId(channel.getIdAsString()).serverId(subscription.getServerName()).date(OffsetDateTime.now())
                .trigger(saleNotification.getTrigger()).contract(saleNotification.getContract()).failed(error).retry(error ? 2 : 0).build());
    }
}
