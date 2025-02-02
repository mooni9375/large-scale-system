package mooni.board.comment.controller;

import lombok.RequiredArgsConstructor;
import mooni.board.comment.service.CommentServiceV2;
import mooni.board.comment.service.request.CommentCreateRequestV2;
import mooni.board.comment.service.response.CommentPageResponse;
import mooni.board.comment.service.response.CommentResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentControllerV2 {

    private final CommentServiceV2 commentService;

    @GetMapping("/v2/comments/{commentId}")
    public CommentResponse read(
            @PathVariable("commentId") Long commentId
    ) {
        return commentService.read(commentId);
    }

    @PostMapping("/v2/comments")
    public CommentResponse create(@RequestBody CommentCreateRequestV2 request) {
        return commentService.create(request);
    }

    @DeleteMapping("/v2/comments/{commentId}")
    public void delete(@PathVariable("commentId") Long commentId) {
        commentService.delete(commentId);
    }

}
