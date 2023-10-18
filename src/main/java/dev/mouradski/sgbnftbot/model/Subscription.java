package dev.mouradski.sgbnftbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Entity
@IdClass(SubscriptionId.class)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class Subscription {
    @Id
    private String contract;
    @Id
    private String channelId;
    private String tokenName;
    private String serverName;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        var oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        var thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Subscription that = (Subscription) o;
        return getContract() != null && Objects.equals(getContract(), that.getContract())
                && getChannelId() != null && Objects.equals(getChannelId(), that.getChannelId());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(contract, channelId);
    }
}
