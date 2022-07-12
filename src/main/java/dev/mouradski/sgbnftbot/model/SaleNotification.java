package dev.mouradski.sgbnftbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SaleNotification {
    private Set<Subscription> subscriptions;
    private String contract;
    private String trigger;
    private Double price;
    private Long tokenId;
    private String marketplaceListingUrl;
    private Marketplace marketplace;
    private TransactionType transactionType;
    private String buyer;
    private String seller;
    private String transactionHash;

    private String imageUrl;
    private byte[] imageContent;
}
