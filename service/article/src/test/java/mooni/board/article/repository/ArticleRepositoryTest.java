package mooni.board.article.repository;

import lombok.extern.slf4j.Slf4j;
import mooni.board.article.entity.Article;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.request.AsyncWebRequestInterceptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class ArticleRepositoryTest {

    ArticleRepository articleRepository;
    @Autowired
    private AsyncWebRequestInterceptor asyncWebRequestInterceptor;

    @Autowired
    public ArticleRepositoryTest(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Test
    void findAllTest() {
        List<Article> articles = articleRepository.findAll(1L, 1499970L, 30L);
        log.info("articles.size: {}", articles.size());
        articles.stream().forEach(article -> {
            log.info("article: {}", article);
        });
    }

    @Test
    void countTest() {
        Long count = articleRepository.count(1L, 10000L);
        log.info("count: {}", count);
    }

    @Test
    void findInfiniteScrollTest() {
        List<Article> articles = articleRepository.findAllInfiniteScroll(1L, 30L);
        for (Article article : articles) {
            log.info("articleId = {}", article.getArticleId());
        }

        Long lastArticle = articles.getLast().getArticleId();
        articleRepository.findAllInfiniteScroll(1L, 30L, lastArticle);
        for (Article article : articles) {
            log.info("articleId = {}", article.getArticleId());
        }
    }
}