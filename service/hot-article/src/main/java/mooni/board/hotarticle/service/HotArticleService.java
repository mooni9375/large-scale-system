package mooni.board.hotarticle.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mooni.board.common.event.Event;
import mooni.board.common.event.EventPayload;
import mooni.board.common.event.EventType;
import mooni.board.hotarticle.client.ArticleClient;
import mooni.board.hotarticle.repository.HotArticleListRepository;
import mooni.board.hotarticle.service.eventhandler.EventHandler;
import mooni.board.hotarticle.service.response.HotArticleResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotArticleService {

    private final ArticleClient articleClient;
    private final List<EventHandler> eventHandlers; // List : EventHandler 내의 모든 의존성이 주입 됨
    private final HotArticleScoreUpdater hotArticleScoreUpdater;
    private final HotArticleListRepository hotArticleListRepository;

    public void handleEvent(Event<EventPayload> event) {

        // 1. EventHandler 찾기
        EventHandler<EventPayload> eventHandler = findEventHandler(event);
        if (eventHandler == null) {
            return;
        }

        // 2. 게시글에 대한 생성 or 삭제 이벤트인지 체크
        if (isArticleCreatedOrDeleted(event)) {

            // 3_1. 생성 or 삭제 이벤트라면 handle
            eventHandler.handle(event);

        } else {

            // 3_2. 생성 or 삭제 이벤트가 아니라면 인기글 update
            hotArticleScoreUpdater.update(event, eventHandler);
        }

    }

    private EventHandler<EventPayload> findEventHandler(Event<EventPayload> event) {
        return eventHandlers.stream()
                .filter(eventHandler -> eventHandler.supports(event))
                .findAny()
                .orElseGet(null);
    }

    private boolean isArticleCreatedOrDeleted(Event<EventPayload> event) {
        return EventType.ARTICLE_CREATED == event.getType() || EventType.ARTICLE_DELETED == event.getType();

    }

    private List<HotArticleResponse> readAll(String dateStr) {

        // yyyyMMdd
        return hotArticleListRepository.readAll(dateStr).stream()
                .map(articleClient::read)
                .filter(Objects::nonNull)
                .map(HotArticleResponse::from)
                .toList();
    }


}
