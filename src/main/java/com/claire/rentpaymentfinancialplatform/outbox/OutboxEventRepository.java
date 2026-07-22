package com.claire.rentpaymentfinancialplatform.outbox;

import com.claire.rentpaymentfinancialplatform.shared.domain.OutboxEventStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID>, JpaSpecificationExecutor<OutboxEvent> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    List<OutboxEvent> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            OutboxEventStatus status,
            Instant nextAttemptAt
    );

    @Query(
            value = """
                    select *
                    from outbox_events
                    where status = 'PENDING'
                      and next_attempt_at <= :now
                    order by next_attempt_at asc, created_at asc
                    limit :limit
                    for update skip locked
                    """,
            nativeQuery = true
    )
    List<OutboxEvent> findEligibleForPublishing(@Param("now") Instant now, @Param("limit") int limit);
}
