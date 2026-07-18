package com.claire.rentpaymentfinancialplatform.outbox;

import com.claire.rentpaymentfinancialplatform.shared.domain.OutboxEventStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    List<OutboxEvent> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            OutboxEventStatus status,
            Instant nextAttemptAt
    );
}
