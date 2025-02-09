package mooni.board.like.service;

import mooni.board.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import mooni.board.like.entity.ArticleLike;
import mooni.board.like.entity.ArticleLikeCount;
import mooni.board.like.repository.ArticleLikeCountRepository;
import mooni.board.like.repository.ArticleLikeRepository;
import mooni.board.like.service.response.ArticleLikeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleLikeService {

    private final Snowflake snowflake = new Snowflake();
    private final ArticleLikeRepository articleLikeRepository;
    private final ArticleLikeCountRepository articleLikeCountRepository;

    public ArticleLikeResponse read(Long articleId, Long userId) {

        return articleLikeRepository.findByArticleIdAndUserId(articleId, userId)
                .map(ArticleLikeResponse::from)
                .orElseThrow();
    }

    /**
     * update 구문
     */
    @Transactional
    public void likePessimisticLock1(Long articleId, Long userId) {

        articleLikeRepository.save(
                ArticleLike.create(
                        snowflake.nextId(),
                        articleId,
                        userId
                )
        );

        int result = articleLikeCountRepository.increase(articleId);
        if (result == 0) {
            // 최초 요청 시에는 update 되는 레코드가 없으므로, 1로 초기화한다.
            // 그러나, 트래픽이 순식간에 몰릴 수 있는 상황에서 유실될 수 있으므로,
            // 게시글 생성 시점에 미리 0으로 초기화하는 전략도 활용 가능하다.
            articleLikeCountRepository.save(
                    ArticleLikeCount.init(articleId, 1L)
            );
        }

    }

    @Transactional
    public void unlikePessimisticLock1(Long articleId, Long userId) {

        articleLikeRepository.findByArticleIdAndUserId(articleId, userId)
                .ifPresent(articleLike -> {
                    articleLikeRepository.delete(articleLike);
                    articleLikeCountRepository.decrease(articleId);
                });
    }

    /**
     * select ... for update + update
     */
    @Transactional
    public void likePessimisticLock2(Long articleId, Long userId) {

        articleLikeRepository.save(
                ArticleLike.create(
                        snowflake.nextId(),
                        articleId,
                        userId
                )
        );

        // 최초(init)일 때, 영속 상태가 아닐 수 있으므로, 명시적으로 save() 호출
        ArticleLikeCount articleLikeCount = articleLikeCountRepository.findLockedByArticleId(articleId)
                .orElseGet(() -> ArticleLikeCount.init(articleId, 0L));
        articleLikeCount.increase();
        articleLikeCountRepository.save(articleLikeCount);

    }

    @Transactional
    public void unlikePessimisticLock2(Long articleId, Long userId) {

        articleLikeRepository.findByArticleIdAndUserId(articleId, userId)
                .ifPresent(articleLike -> {
                    articleLikeRepository.delete(articleLike);
                    ArticleLikeCount articleLikeCount = articleLikeCountRepository.findLockedByArticleId(articleId).orElseThrow();
                    articleLikeCount.decrease();
                });
    }

    /**
     * select ... for update + update
     */
    @Transactional
    public void likeOptimisticLock(Long articleId, Long userId) {

        articleLikeRepository.save(
                ArticleLike.create(
                        snowflake.nextId(),
                        articleId,
                        userId
                )
        );

        // 최초(init)일 때, 영속 상태가 아닐 수 있으므로, 명시적으로 save() 호출
        ArticleLikeCount articleLikeCount = articleLikeCountRepository.findById(articleId)
                .orElseGet(() -> ArticleLikeCount.init(articleId, 0L));
        articleLikeCount.increase();
        articleLikeCountRepository.save(articleLikeCount);

    }

    @Transactional
    public void unlikeOptimisticLock(Long articleId, Long userId) {

        articleLikeRepository.findByArticleIdAndUserId(articleId, userId)
                .ifPresent(articleLike -> {
                    articleLikeRepository.delete(articleLike);
                    ArticleLikeCount articleLikeCount = articleLikeCountRepository.findById(articleId).orElseThrow();
                    articleLikeCount.decrease();
                });
    }

    public Long count(Long articleId) {
        return articleLikeCountRepository.findById(articleId)
                .map(ArticleLikeCount::getLikeCount)
                .orElse(0L);
    }


}
