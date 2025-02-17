package mooni.board.common.outboxmessagerelay;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Outbox 테이블을 관리하고, 트랜잭션 종료 후 Kafka로 이벤트를 전송하는 핵심 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRelay {

    private final OutboxRepository outboxRepository;
    private final MessageRelayCoordinator messageRelayCoordinator;
    private final KafkaTemplate<String, String> messageRelayKafkaTemplate;

    /**
     * @TransactionalEventListener
     *  ApplicationEventPublisher를 통해 트랜잭션에 대한 이벤트를 받을 수 있음.
     *  즉,
     *
     * 트랜잭션이 커밋되기 직전에 Outbox 테이블에 이벤트를 저장.
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void createOutbox(OutboxEvent outboxEvent) {
        log.info("[MessageRelay.createOutbox] outboxEvent = {}", outboxEvent);
        outboxRepository.save(outboxEvent.getOutbox());
    }

    /**
     *  MessageRelayConfig 내에 @EnableAsync 선언되어 있어야 작동
     *
     *  트랜잭션이 성공적으로 커밋되면 Kafka로 비동기 메시지 전송.
     */
    @Async("messageRelayPublishEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishEvent(OutboxEvent outboxEvent) {
        publishEvent(outboxEvent.getOutbox());
    }

    /**
     * Kafka로 메시지를 전송한 후, 성공적으로 전송되면 Outbox 테이블에서 삭제.
     */
    private void publishEvent(Outbox outbox) {

        try {
            messageRelayKafkaTemplate.send(
                    outbox.getEventType().getTopic(),
                    String.valueOf(outbox.getShardKey()),
                    outbox.getPayload()
            ).get(1, TimeUnit.SECONDS);

            outboxRepository.delete(outbox);

        } catch (Exception e) {
            log.error("[MessageRelay.publishEvent] outbox = {}", outbox, e);
        }

    }

    /**
     *  MessageRelayConfig 내에 @EnableScheduling 선언되어 있어야 작동
     *
     *  전송되지 않은 메시지들을 주기적으로 kafka로 보내기 위한 메서드
     */
    @Scheduled(
            fixedDelay = 10,
            initialDelay = 5,
            timeUnit = TimeUnit.SECONDS,
            scheduler = "messageRelayPublishPendingEventExecutor"
    )
    public void publishPendingEvent() {

        AssignedShard assignedShard = messageRelayCoordinator.assignedShards();
        log.info("[MessageRelay.publishPendingEvent] assignedShard size = {}", assignedShard.getShards().size());

        for (Long shard : assignedShard.getShards()) {
            List<Outbox> outboxes = outboxRepository.findAllByShardKeyAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                    shard,
                    LocalDateTime.now().minusSeconds(10),
                    Pageable.ofSize(100)
            );

            for (Outbox outbox : outboxes) {
                publishEvent(outbox);
            }

        }
    }
}
