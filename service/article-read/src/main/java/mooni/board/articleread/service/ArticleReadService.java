package mooni.board.articleread.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mooni.board.articleread.client.ArticleClient;
import mooni.board.articleread.client.CommentClient;
import mooni.board.articleread.client.LikeClient;
import mooni.board.articleread.client.ViewClient;
import mooni.board.articleread.repository.ArticleQueryModel;
import mooni.board.articleread.repository.ArticleQueryModelRepository;
import mooni.board.articleread.service.event.handler.EventHandler;
import mooni.board.articleread.service.response.ArticleReadResponse;
import mooni.board.common.event.Event;
import mooni.board.common.event.EventPayload;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleReadService {

    private final ArticleClient articleClient;
    private final CommentClient commentClient;
    private final LikeClient likeClient;
    private final ViewClient viewClient;
    private final ArticleQueryModelRepository articleQueryModelRepository;

    private final List<EventHandler> eventHandlers;

    public void handleEvent(Event<EventPayload> event) {
        for (EventHandler eventHandler : eventHandlers) {
            if (eventHandler.supports(event)) {
                eventHandler.handle(event);
            }
        }
    }

    public ArticleReadResponse read(Long articleId) {
        ArticleQueryModel articleQueryModel = articleQueryModelRepository.read(articleId)
                .or(() -> fetch(articleId))
                .orElseThrow();

        return ArticleReadResponse.from(
            articleQueryModel,
                viewClient.count(articleId)
        );
    }

    private Optional<ArticleQueryModel> fetch(Long articleId) {
        Optional<ArticleQueryModel> articleQueryModelOptional = articleClient.read(articleId)
                .map(article -> ArticleQueryModel.create(
                        article,
                        commentClient.count(articleId),
                        likeClient.count(articleId)
                ));

        articleQueryModelOptional.ifPresent(articleQueryModel -> {
            articleQueryModelRepository.create(articleQueryModel, Duration.ofDays(1));
        });

        log.info("[ArticleReadService.fetch] fetch data. articleId = {}, ifPresent = {}", articleId, articleQueryModelOptional.isPresent());

        return articleQueryModelOptional;
    }


}
