package mooni.board.hotarticle.service.eventhandler;

import lombok.RequiredArgsConstructor;
import mooni.board.common.event.Event;
import mooni.board.common.event.EventType;
import mooni.board.common.event.payload.ArticleUnlikedEventPayload;
import mooni.board.hotarticle.repository.ArticleLikeCountRepository;
import mooni.board.hotarticle.utils.TimeCalculatorUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleUnlikedEventHandler implements EventHandler<ArticleUnlikedEventPayload> {

    private final ArticleLikeCountRepository articleLikeCountRepository;


    @Override
    public void handle(Event<ArticleUnlikedEventPayload> event) {
        ArticleUnlikedEventPayload payload = event.getPayload();
        articleLikeCountRepository.createOrUpdate(
                payload.getArticleId(),
                payload.getArticleLikeCount(),
                TimeCalculatorUtils.calculateDurationToMidnight()
        );
    }

    @Override
    public boolean supports(Event<ArticleUnlikedEventPayload> event) {
        return EventType.ARTICLE_UNLIKED == event.getType();
    }

    @Override
    public Long findArticleId(Event<ArticleUnlikedEventPayload> event) {
        return event.getPayload().getArticleId();
    }
}
