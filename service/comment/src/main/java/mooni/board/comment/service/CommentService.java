package mooni.board.comment.service;

import kuke.board.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import mooni.board.comment.entity.Comment;
import mooni.board.comment.repository.CommentRepository;
import mooni.board.comment.service.request.CommentCreateRequest;
import mooni.board.comment.service.response.CommentPageResponse;
import mooni.board.comment.service.response.CommentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.function.Predicate.not;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final Snowflake snowflake = new Snowflake();
    private final CommentRepository commentRepository;

    @Transactional
    public CommentResponse create(CommentCreateRequest request) {

        Comment parent  = findParent(request);
        Comment comment = commentRepository.save(
                Comment.create(
                        snowflake.nextId(),
                        request.getContent(),
                        parent == null ? null : parent.getCommentId(),
                        request.getArticleId(),
                        request.getWriterId()
                )
        );
        return CommentResponse.from(comment);
    }

    private Comment findParent(CommentCreateRequest request) {

        Long parentCommentId = request.getParentCommentId();

        if (parentCommentId == null) {
            return null;
        }

        return commentRepository.findById(parentCommentId)
                .filter(not(Comment::getDeleted)) // 상위 댓글이 아직 삭제된 상태가 아니어야 함
                .filter(Comment::isRoot) // 상위 댓글이 루트 댓글이어야 함
                .orElseThrow();
    }

    public CommentResponse read(Long commentId) {
        return CommentResponse.from(
                commentRepository.findById(commentId).orElseThrow()
        );
    }

    @Transactional
    public void delete(Long commentId) {

        commentRepository.findById(commentId)
                .filter(not(Comment::getDeleted)) // 아직 삭제되지 않은 댓글인지 확인
                .ifPresent(comment -> {
                    if (hasChildren(comment)) { // 자식이 있으면, 상태만 변경
                        comment.delete();
                    } else {                    // 자식이 없으면, 삭제
                        delete(comment);
                    }
                });
    }

    private boolean hasChildren(Comment comment) {
        // 자신 + 자식 = 2
        // 자신의 commentId가 자식의 parentCommentId로 사용되는 데이터가 있는지 확인
        return commentRepository.countBy(comment.getArticleId(), comment.getCommentId(), 2L) == 2;
    }

    private void delete(Comment comment) {

        commentRepository.delete(comment);

        // 삭제 대상 댓글이 자식 댓글인 경우, 부모 댓글이 이미 삭제되었고, 부모 댓글의 다른 자식 댓글이 없다면, 부모 댓글도 삭제
        if (!comment.isRoot()) {
            commentRepository.findById(comment.getParentCommentId())
                    .filter(Comment::getDeleted) // 부모 댓글이 이미 삭제된 상태인지 확인
                    .filter(not(this::hasChildren)) // 부모 댓글이 자식을 가지고 않은지 확인
                    .ifPresent(this::delete); // 부모 댓글을 삭제할 수 있는 상태면 재귀적으로 삭제
        }
    }

    public CommentPageResponse readAll(Long articleId, Long page, Long pageSize) {
        return CommentPageResponse.of(
                commentRepository.findAll(articleId, (page - 1) * pageSize, pageSize).stream()
                        .map(CommentResponse::from)
                        .toList(),
                commentRepository.count(articleId, PageLimitCalculator.calculatePageLimit(page, pageSize, 10L))
        );
    }

    public List<CommentResponse> readAll(Long articleId, Long lastParentCommentId, Long lastCommentId, Long limit) {
        List<Comment> comments = lastParentCommentId == null || lastCommentId == null ?
                commentRepository.findAllInfiniteScroll(articleId, limit) :
                commentRepository.findAllInfiniteScroll(articleId, lastParentCommentId, lastCommentId, limit);
        return comments.stream()
                .map(CommentResponse::from)
                .toList();

    }

}
