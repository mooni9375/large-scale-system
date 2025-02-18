package mooni.board.articleread.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mooni.board.articleread.service.ArticleReadService;
import mooni.board.common.event.Event;
import mooni.board.common.event.EventPayload;
import mooni.board.common.event.EventType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleReadEventConsumer {

    private final ArticleReadService articleReadService;

    @KafkaListener(topics = {
            EventType.Topic.MOONI_BOARD_ARTICLE,
            EventType.Topic.MOONI_BOARD_COMMENT,
            EventType.Topic.MOONI_BOARD_LIKE,
    })
    public void listen(String message, Acknowledgment ack) {
        log.info("[ArticleReadEventConsumer.listen] message = {}", message);

        Event<EventPayload> event = Event.fromJson(message);

        if (event != null) {
            articleReadService.handleEvent(event);
        }

        ack.acknowledge();
    }
}
