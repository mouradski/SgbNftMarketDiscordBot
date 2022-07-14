package dev.mouradski.sgbnftbot.repository;

import dev.mouradski.sgbnftbot.model.Subscription;
import dev.mouradski.sgbnftbot.model.SubscriptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, SubscriptionId> {
    List<Subscription> findByContract(String contract);
    List<Subscription> findByChannelId(String channelId);
}
