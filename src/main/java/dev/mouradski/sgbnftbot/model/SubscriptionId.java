package dev.mouradski.sgbnftbot.model;

import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubscriptionId implements Serializable {
    private String contract;
    private String channelId;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        var oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        var thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        SubscriptionId that = (SubscriptionId) o;
        return getContract() != null && Objects.equals(getContract(), that.getContract())
                && getChannelId() != null && Objects.equals(getChannelId(), that.getChannelId());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(contract, channelId);
    }
}
