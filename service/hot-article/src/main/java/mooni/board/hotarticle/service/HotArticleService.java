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
    // EventHandler를 구현한 모든 빈(@Component, @Service...)에 대해 의존성 주입
    private final List<EventHandler> eventHandlers;
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

            // 3_2. 생성 or 삭제 이벤트가 아니라면 인기글 update (updater에서 redis를 통해 메모리에 적재)
            hotArticleScoreUpdater.update(event, eventHandler);
        }

    }

    /**
     *  filter : Returns a stream consisting of the results of applying the given function to the elements of this stream.
     *  findAny : RReturns an Optional describing some element of the stream, or an empty Optional if the stream is empty.
     */
    private EventHandler<EventPayload> findEventHandler(Event<EventPayload> event) {
        return eventHandlers.stream()
                .filter(eventHandler -> eventHandler.supports(event))
                .findAny()
                .orElse(null);
    }

    private boolean isArticleCreatedOrDeleted(Event<EventPayload> event) {
        return EventType.ARTICLE_CREATED == event.getType() || EventType.ARTICLE_DELETED == event.getType();

    }

    public List<HotArticleResponse> readAll(String dateStr) {

        // yyyyMMdd
        return hotArticleListRepository.readAll(dateStr).stream()
                .map(articleClient::read)
                .filter(Objects::nonNull)
                .map(HotArticleResponse::from)
                .toList();
    }


}
