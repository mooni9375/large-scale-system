package mooni.board.view.service;

import lombok.RequiredArgsConstructor;
import mooni.board.view.entity.ArticleViewCount;
import mooni.board.view.repository.ArticleViewCountBackUpRepository;
import mooni.board.view.repository.ArticleViewCountRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ArticleViewCountBackUpProcessor {

    private final ArticleViewCountBackUpRepository articleViewCountBackUpRepository;

    @Transactional
    public void backUp(Long articleId, Long viewCount) {

        int result = articleViewCountBackUpRepository.updateViewCount(articleId, viewCount);

        if (result == 0) {
            articleViewCountBackUpRepository.findById(articleId)
                    .ifPresentOrElse(articleViewCountBackUp -> {},
                            () -> articleViewCountBackUpRepository.save(ArticleViewCount.init(articleId, viewCount))
                    );
        }
    }

}
