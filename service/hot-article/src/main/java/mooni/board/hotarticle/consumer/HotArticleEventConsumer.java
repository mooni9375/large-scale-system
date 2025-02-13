package mooni.board.hotarticle.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mooni.board.common.event.Event;
import mooni.board.common.event.EventPayload;
import mooni.board.common.event.EventType;
import mooni.board.hotarticle.service.HotArticleService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotArticleEventConsumer {

    private final HotArticleService hotArticleService;

    @KafkaListener(topics = {
            EventType.Topic.MOONI_BOARD_ARTICLE,
            EventType.Topic.MOONI_BOARD_COMMENT,
            EventType.Topic.MOONI_BOARD_LIKE,
            EventType.Topic.MOONI_BOARD_VIEW,
    })
    public void listen(String message, Acknowledgment ack) {
        log.info("[HotArticleEventConsumer.listen] received message = {}", message);
        Event<EventPayload> event = Event.fromJson(message);

        if (event != null) {
            hotArticleService.handleEvent(event);
        }

        // 메시지가 잘 처리되었음을 카프카에 알림
        ack.acknowledge();
    }
}
