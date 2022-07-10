package dev.mouradski.sgbnftbot.repository;

import dev.mouradski.sgbnftbot.model.SaleNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
@Transactional
public interface SaleNotificationLogRepository extends JpaRepository<SaleNotificationLog, Long> {
    void deleteByDateBefore(OffsetDateTime date);
    List<SaleNotificationLog> findByContract(String contract);
    List<SaleNotificationLog> findByTransactionHash(String transactionHash);
    List<SaleNotificationLog> findByContractAndChannelId(String contract, String channelId);
}
