package mooni.board.article.service;

import mooni.board.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import mooni.board.article.entity.Article;
import mooni.board.article.entity.BoardArticleCount;
import mooni.board.article.repository.ArticleRepository;
import mooni.board.article.repository.BoardArticleCountRepository;
import mooni.board.article.service.request.ArticleCreateRequest;
import mooni.board.article.service.request.ArticleUpdateRequest;
import mooni.board.article.service.response.ArticlePageResponse;
import mooni.board.article.service.response.ArticleResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final Snowflake snowflake = new Snowflake();
    private final ArticleRepository articleRepository;
    private final BoardArticleCountRepository boardArticleCountRepository;

//    @Autowired
//    public ArticleService(ArticleRepository articleRepository) {
//        this.articleRepository = articleRepository;
//    }

    @Transactional
    public ArticleResponse create(ArticleCreateRequest request) {
        Article article = articleRepository.save(
                Article.create(snowflake.nextId(),
                        request.getTitle(),
                        request.getContent(),
                        request.getBoardId(),
                        request.getWriterId())
        );
        int result = boardArticleCountRepository.increase(article.getBoardId());
        if (result == 0) {
            boardArticleCountRepository.save(
                    BoardArticleCount.init(request.getBoardId(), 1L)
            );
        }
        return ArticleResponse.from(article);
    }

    /**
     * JPA 변경감지(Dirty Checking)
     *  findById() : 조회 후 영속성 컨테이너에서 엔터티 관리
     *  update()   : 변경 감지 및 UPDATE 쿼리 생성
     *  @Transactional : 트랜잭션 커밋 시점에 영속성 컨텍스트의 변경된 엔터티를 모두 검사하고 최종 쿼리 실행
     */
    @Transactional
    public ArticleResponse update(Long articleId, ArticleUpdateRequest request) {
        Article article = articleRepository.findById(articleId).orElseThrow();
        article.update(request.getTitle(), request.getContent());

        return ArticleResponse.from(article);
    }

    public ArticleResponse read(Long articleId) {
        return ArticleResponse.from(articleRepository.findById(articleId).orElseThrow());
    }

    @Transactional
    public void delete(Long articleId) {
        Article article = articleRepository.findById(articleId).orElseThrow();
        articleRepository.delete(article);
        boardArticleCountRepository.decrease(article.getBoardId());
    }

    public ArticlePageResponse readAll(Long boardId, Long page, Long pageSize) {
        return ArticlePageResponse.of(
                articleRepository.findAll(boardId, (page - 1) * pageSize, pageSize).stream()
                        .map(ArticleResponse::from)
                        .toList(),
                articleRepository.count(
                        boardId,
                        PageLimitCalculator.calculatePageLimit(page, pageSize, 10L)
                )
        );
    }

    public List<ArticleResponse> readAllInfiniteScroll(Long boardId, Long pageSize, Long lastArticleId) {
        List<Article> articles = lastArticleId == null ?
                articleRepository.findAllInfiniteScroll(boardId, pageSize) :
                articleRepository.findAllInfiniteScroll(boardId, pageSize, lastArticleId);

        return articles.stream().map(ArticleResponse::from).toList();
    }

    public Long count(Long boardId) {
        return boardArticleCountRepository.findById(boardId)
                .map(BoardArticleCount::getArticleCount)
                .orElse(0L);
    }
}
