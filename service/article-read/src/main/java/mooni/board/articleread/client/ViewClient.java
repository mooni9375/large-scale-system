package mooni.board.articleread.client;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

/**
 * REST API Client
 *  endpoints(mooni-board-view-service)에서 게시글 데이터를 가져옴.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewClient {

    private RestClient restClient;

    @Value("${endpoints.mooni-board-view-service.url}")
    private String viewServiceUrl;

    @PostConstruct
    public void initRestClient() {
        restClient = RestClient.create(viewServiceUrl);
    }

    /**
     * Redis에서 데이터를 조회한다.
     *
     *  데이터가 있다면 그 데이터를 그대로 반환한다.
     *
     *  데이터가 없다면 count 메서드 내부로직이 호출되면서 viewService로 원본 데이터를 요청한다.
     *  그리고 Redis에 데이터를 적재하고 응답한다.
     */
    @Cacheable(key = "#articleId", value = "articleViewCount")
    public long count(Long articleId) {
        log.info("[ViewClient.count] articleId: {}", articleId);

        try {
            return restClient.get()
                    .uri("/v1/article-views/articles/{articleId}/count", articleId)
                    .retrieve()
                    .body(Long.class);
        } catch (Exception e) {
            log.error("[ViewClient.count] articleId = {}", articleId, e);
            return 0;
        }
    }
}
