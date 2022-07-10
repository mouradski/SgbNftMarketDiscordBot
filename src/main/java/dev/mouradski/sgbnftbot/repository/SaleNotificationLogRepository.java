package dev.mouradski.sgbnftbot.repository;

import dev.mouradski.sgbnftbot.model.SaleNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface SaleNotificationLogRepository extends JpaRepository<SaleNotificationLog, Long> {
    void deleteByDateBefore(OffsetDateTime date);
    List<SaleNotificationLog> findByFailedIsTrueAndRetryGreaterThan(Integer retry);
    List<SaleNotificationLog> findByContract(String contract);
    List<SaleNotificationLog> findByTransactionHash(String transactionHash);
    List<SaleNotificationLog> findByContractAndChannelIdAndRetryGreaterThan(String contract, String channelId, Integer retry);
    Optional<SaleNotificationLog> getByTransactionHashAndChannelIdAndFailedIsTrue(String transactionHash, String channelId);

    @Modifying
    @Query("update SaleNotificationLog sn set sn.retry = 0, sn.failed = false where sn.transactionHash = ?1 and sn.channelId = ?2")
    void stopRetry(String transactionHash, String channelId);
}
