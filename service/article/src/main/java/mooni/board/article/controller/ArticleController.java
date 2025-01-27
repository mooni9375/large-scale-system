package mooni.board.article.controller;

import lombok.RequiredArgsConstructor;
import mooni.board.article.service.ArticleService;
import mooni.board.article.service.request.ArticleCreateRequest;
import mooni.board.article.service.request.ArticleUpdateRequest;
import mooni.board.article.service.response.ArticleResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

//    @Autowired
//    public ArticleController(ArticleService articleService) {
//        this.articleService = articleService;
//    }

    @GetMapping("/v1/articles/{articleId}")
    public ArticleResponse getArticle(@PathVariable Long articleId) {
        return articleService.read(articleId);
    }

    @PostMapping("/v1/articles")
    public ArticleResponse create(@RequestBody ArticleCreateRequest request) {
        return articleService.create(request);
    }

    @PutMapping("/v1/articles/{articleId}")
    public ArticleResponse update(@PathVariable Long articleId, @RequestBody ArticleUpdateRequest request) {
        return articleService.update(articleId, request);
    }

    @DeleteMapping("/v1/articles/{articleId}")
    public void delete(@PathVariable Long articleId) {
        articleService.delete(articleId);
    }
}
