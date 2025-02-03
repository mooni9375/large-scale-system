package mooni.board.like.service;

import kuke.board.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import mooni.board.like.entity.ArticleLike;
import mooni.board.like.repository.ArticleLikeRepository;
import mooni.board.like.service.response.ArticleLikeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleLikeService {

    private final Snowflake snowflake = new Snowflake();
    private final ArticleLikeRepository articleLikeRepository;

    public ArticleLikeResponse read(Long articleId, Long userId) {

        return articleLikeRepository.findByArticleIdAndUserId(articleId, userId)
                .map(ArticleLikeResponse::from)
                .orElseThrow();
    }

    @Transactional
    public void like(Long articleId, Long userId) {

        articleLikeRepository.save(
                ArticleLike.create(
                        snowflake.nextId(),
                        articleId,
                        userId
                )
        );
    }

    @Transactional
    public void unlike(Long articleId, Long userId) {

        articleLikeRepository.findByArticleIdAndUserId(articleId, userId)
                .ifPresent(articleLikeRepository::delete);
    }


}
