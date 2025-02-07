package com.example.footballwebproject.comment.service;

import com.example.footballwebproject.comment.dto.CommentRequestDto;
import com.example.footballwebproject.comment.dto.CommentResponseDto;
import com.example.footballwebproject.comment.dto.ReplyResponseDto;
import com.example.footballwebproject.comment.repository.CommentRepository;
import com.example.footballwebproject.entity.Comment;
import com.example.footballwebproject.entity.Game;
import com.example.footballwebproject.entity.User;
import com.example.footballwebproject.exception.ApiException;
import com.example.footballwebproject.exception.ExceptionEnum;
import com.example.footballwebproject.game.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private static final Long PARENT_COMMENT_PATH_LENGTH = 2L;
    private final CommentRepository commentRepository;
    private final GameRepository gameRepository;

    /**
     * 최상위 부모 댓글 작성
     */
    @Transactional
    public CommentResponseDto addComment(Long gameId, CommentRequestDto commentRequestDto, User user) {
        Game game = gameRepository.findById(gameId).orElseThrow(
                () -> new ApiException(ExceptionEnum.GAME_NOT_FOUND)
        );

        Comment comment = commentRepository.save(new Comment(commentRequestDto, game, user));

        // 최상위 댓글이므로 path 정보 업데이트
        String path = comment.getId().toString() + "/";
        comment.updatePath(path);
        return new CommentResponseDto(comment);
    }

    /**
     * 최상위 부모 댓글 수정
     */
    @Transactional
    public String updateComment(Long commentId, CommentRequestDto commentRequestDto, User user) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new ApiException(ExceptionEnum.COMMENT_NOT_FOUND)
        );

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new ApiException(ExceptionEnum.INVALID_USER_UPDATE);
        }
        // 로그인 한 유저는 소문자유저 comment 유저는 데이터베이스 에 가져오는 칼럼을 가져오게 되면 서로 일치하면 수정 가능
        comment.update(commentRequestDto);
        CommentResponseDto responseDto = new CommentResponseDto(comment);

        return "댓글 수정 성공";
    }

    /**
     * 최상위 부모 댓글 삭제
     */
    @Transactional
    public String deleteComment(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new ApiException(ExceptionEnum.COMMENT_NOT_FOUND)
        );

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new ApiException(ExceptionEnum.INVALID_USER_DELETE);
        }

        // 경로 바탕으로 자식 댓글을 찾아서 모두 삭제
        String path = comment.getPath();
        commentRepository.deleteAllByPathStartsWith(path);

        return "댓글 삭제 성공";
    }

    /**
     * 대댓글 작성
     */
    @Transactional
    public ReplyResponseDto addReply(Long commentId, Long gameId, CommentRequestDto commentRequestDto, User user) {
        Game game = gameRepository.findById(gameId).orElseThrow(
                () -> new ApiException(ExceptionEnum.GAME_NOT_FOUND)
        );

        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new ApiException(ExceptionEnum.COMMENT_NOT_FOUND)
        );

        // 1계층 대댓글 요구 사항에 따른 제한조건 구현
        // 경로 문자열 길이가 특정 길이 이상이면 throw
        // "1/" 은 길이가 2이므로 부모댓글임
        // "1/2/" 는 길이가 2 이상이므로 대댓글임
        if (comment.getPath().length() > PARENT_COMMENT_PATH_LENGTH) {
            throw new ApiException(ExceptionEnum.NOT_COMMENT_REPLY);
        }

        Comment reply = commentRepository.save(new Comment(commentRequestDto, game, user, comment.getDepth() + 1));

        // 경로 갱신
        String path = comment.getPath() + reply.getId() + "/";
        reply.updatePath(path);

        return new ReplyResponseDto(reply);
    }

    /**
     * 최상위 부모 댓글의 reply 조회
     * 특정 최상위 부모 댓글의 대댓글을 조회함
     */
    @Transactional
    public List<ReplyResponseDto> getReply(Long commentId) {
        List<Comment> replyList = commentRepository.findAllByPathStartsWith(commentId + "/");
        return replyList.stream().map(ReplyResponseDto::new).toList();
    }

    /**
     * 대댓글 수정하기
     */
    @Transactional
    public ReplyResponseDto updateReply(Long commentId, Long replyId, CommentRequestDto commentRequestDto, User user) {
        Comment reply = commentRepository.findById(replyId).orElseThrow(
                () -> new ApiException(ExceptionEnum.COMMENT_NOT_FOUND)
        );

        if (!reply.getUser().getId().equals(user.getId())) {
            throw new ApiException(ExceptionEnum.INVALID_USER_UPDATE);
        }

        reply.update(commentRequestDto);
        return new ReplyResponseDto(reply);
    }

    /**
     * 대댓글 삭제하기
     * 쿼리 확인 결과 비효율적임
     */
    @Transactional
    public String deleteReply(Long replyId, User user) {
        Comment reply = commentRepository.findById(replyId).orElseThrow(
                () -> new ApiException(ExceptionEnum.COMMENT_NOT_FOUND)
        );

        if (!reply.getUser().getId().equals(user.getId())) {
            throw new ApiException(ExceptionEnum.INVALID_USER_DELETE);
        }

        // 경로 바탕으로 자식 댓글을 찾아서 모두 삭제
        String path = reply.getPath();
        commentRepository.deleteAllByPathStartsWith(path);
        return "대댓글 삭제 성공";
    }
}

