package dev.mouradski.sgbnftbot.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubscriptionId implements Serializable {
    private String contract;
    private String channelId;
}
