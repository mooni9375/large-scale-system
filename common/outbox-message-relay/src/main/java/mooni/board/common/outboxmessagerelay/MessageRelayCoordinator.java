package mooni.board.common.outboxmessagerelay;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Control Sharding
 */
@Component
@RequiredArgsConstructor
public class MessageRelayCoordinator {

    private final StringRedisTemplate redisTemplate;

    // each services name : ex) mooni-board-article-service
    @Value("{spring.application.name}")
    private String applicationName;

    private final String APP_ID = UUID.randomUUID().toString();

    // 3초 간격으로 조회 & 3회 실패시 애플리케이션이 죽었다고 판단
    private final int PING_INTERVAL_SECONDS = 3;
    private final int PING_FAILURE_THRESHOLD = 3;

    public AssignedShard assignedShards() {
        return AssignedShard.of(APP_ID, findAppIds(), MessageRelayConstants.SHARD_COUNT);
    }

    private List<String> findAppIds() {
        return redisTemplate.opsForZSet().reverseRange(generateKey(), 0, -1).stream()
                .sorted()
                .toList();
    }

    @Scheduled(fixedDelay = PING_INTERVAL_SECONDS, timeUnit = TimeUnit.SECONDS)
    public void ping() {

        // redisTemplate.executePipelined() : 한 번의 통신으로 여러 연산을 한꺼번에 redis로 전달
        redisTemplate.executePipelined((RedisCallback<?>) action -> {
            StringRedisConnection conn = (StringRedisConnection) action;
            String key = generateKey();
            conn.zAdd(key, Instant.now().toEpochMilli(), APP_ID);
            conn.zRemRangeByScore(
                    key,
                    Double.NEGATIVE_INFINITY,
                    Instant.now().minusSeconds(PING_INTERVAL_SECONDS * PING_FAILURE_THRESHOLD).toEpochMilli()
            );
            return null;
        });
    }

    @PreDestroy
    public void leave() {
        redisTemplate.opsForZSet().remove(generateKey(), APP_ID);
    }

    private String generateKey() {
        return "message-relay-coordinator::app-list::%s".formatted(applicationName);
    }
}
