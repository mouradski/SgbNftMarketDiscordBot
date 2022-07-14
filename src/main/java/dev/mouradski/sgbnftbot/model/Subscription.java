package dev.mouradski.sgbnftbot.model;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@IdClass(SubscriptionId.class)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Subscription {
    @Id
    private String contract;
    @Id
    private String channelId;
    private String tokenName;
    private String serverName;
}
