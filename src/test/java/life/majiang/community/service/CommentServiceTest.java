package life.majiang.community.service;

import life.majiang.community.dto.CommentDTO;
import life.majiang.community.enums.CommentTypeEnum;
import life.majiang.community.exception.CustomizeErrorCode;
import life.majiang.community.exception.CustomizeException;
import life.majiang.community.mapper.CommentExtMapper;
import life.majiang.community.mapper.CommentMapper;
import life.majiang.community.mapper.NotificationMapper;
import life.majiang.community.mapper.QuestionExtMapper;
import life.majiang.community.mapper.QuestionMapper;
import life.majiang.community.mapper.UserMapper;
import life.majiang.community.model.Comment;
import life.majiang.community.model.Notification;
import life.majiang.community.model.Question;
import life.majiang.community.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private QuestionExtMapper questionExtMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CommentExtMapper commentExtMapper;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private CommentService commentService;

    private User commentator(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }

    @Test
    void insert_whenParentIdNull_throwsTargetParamNotFound() {
        Comment comment = new Comment();
        comment.setParentId(null);
        comment.setType(CommentTypeEnum.QUESTION.getType());

        CustomizeException ex = assertThrows(CustomizeException.class,
                () -> commentService.insert(comment, commentator(1L, "bob")));
        assertEquals(CustomizeErrorCode.TARGET_PARAM_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void insert_whenParentIdZero_throwsTargetParamNotFound() {
        Comment comment = new Comment();
        comment.setParentId(0L);
        comment.setType(CommentTypeEnum.QUESTION.getType());

        CustomizeException ex = assertThrows(CustomizeException.class,
                () -> commentService.insert(comment, commentator(1L, "bob")));
        assertEquals(CustomizeErrorCode.TARGET_PARAM_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void insert_whenTypeNull_throwsTypeParamWrong() {
        Comment comment = new Comment();
        comment.setParentId(1L);
        comment.setType(null);

        CustomizeException ex = assertThrows(CustomizeException.class,
                () -> commentService.insert(comment, commentator(1L, "bob")));
        assertEquals(CustomizeErrorCode.TYPE_PARAM_WRONG.getCode(), ex.getCode());
    }

    @Test
    void insert_whenTypeInvalid_throwsTypeParamWrong() {
        Comment comment = new Comment();
        comment.setParentId(1L);
        comment.setType(99);

        CustomizeException ex = assertThrows(CustomizeException.class,
                () -> commentService.insert(comment, commentator(1L, "bob")));
        assertEquals(CustomizeErrorCode.TYPE_PARAM_WRONG.getCode(), ex.getCode());
    }

    @Test
    void insert_replyToComment_success_incrementsCountAndNotifies() {
        Comment comment = new Comment();
        comment.setParentId(10L);
        comment.setType(CommentTypeEnum.COMMENT.getType());
        comment.setCommentator(3000L);

        Comment dbComment = new Comment();
        dbComment.setId(10L);
        dbComment.setParentId(20L);
        dbComment.setCommentator(2000L);

        Question question = new Question();
        question.setId(20L);
        question.setCreator(999L);
        question.setTitle("Q");

        when(commentMapper.selectByPrimaryKey(10L)).thenReturn(dbComment);
        when(questionMapper.selectByPrimaryKey(20L)).thenReturn(question);

        commentService.insert(comment, commentator(3000L, "bob"));

        verify(commentMapper).insert(comment);
        verify(commentExtMapper).incCommentCount(any(Comment.class));
        verify(notificationMapper).insert(any(Notification.class));
        verify(questionExtMapper, never()).incCommentCount(any(Question.class));
    }

    @Test
    void insert_replyToComment_whenParentCommentMissing_throwsCommentNotFound() {
        Comment comment = new Comment();
        comment.setParentId(10L);
        comment.setType(CommentTypeEnum.COMMENT.getType());
        comment.setCommentator(3000L);
        when(commentMapper.selectByPrimaryKey(10L)).thenReturn(null);

        CustomizeException ex = assertThrows(CustomizeException.class,
                () -> commentService.insert(comment, commentator(3000L, "bob")));
        assertEquals(CustomizeErrorCode.COMMENT_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void insert_replyToComment_whenQuestionMissing_throwsQuestionNotFound() {
        Comment comment = new Comment();
        comment.setParentId(10L);
        comment.setType(CommentTypeEnum.COMMENT.getType());
        comment.setCommentator(3000L);

        Comment dbComment = new Comment();
        dbComment.setId(10L);
        dbComment.setParentId(20L);
        dbComment.setCommentator(2000L);

        when(commentMapper.selectByPrimaryKey(10L)).thenReturn(dbComment);
        when(questionMapper.selectByPrimaryKey(20L)).thenReturn(null);

        CustomizeException ex = assertThrows(CustomizeException.class,
                () -> commentService.insert(comment, commentator(3000L, "bob")));
        assertEquals(CustomizeErrorCode.QUESTION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void insert_replyToQuestion_success_incrementsCountAndNotifies() {
        Comment comment = new Comment();
        comment.setParentId(20L);
        comment.setType(CommentTypeEnum.QUESTION.getType());
        comment.setCommentator(3000L);

        Question question = new Question();
        question.setId(20L);
        question.setCreator(2000L);
        question.setTitle("Q");

        when(questionMapper.selectByPrimaryKey(20L)).thenReturn(question);

        commentService.insert(comment, commentator(3000L, "bob"));

        verify(commentMapper).insert(comment);
        verify(questionExtMapper).incCommentCount(any(Question.class));
        verify(notificationMapper).insert(any(Notification.class));
        verify(commentExtMapper, never()).incCommentCount(any(Comment.class));
        assertEquals(0, comment.getCommentCount().intValue());
    }

    @Test
    void insert_replyToQuestion_whenQuestionMissing_throwsQuestionNotFound() {
        Comment comment = new Comment();
        comment.setParentId(20L);
        comment.setType(CommentTypeEnum.QUESTION.getType());
        comment.setCommentator(3000L);
        when(questionMapper.selectByPrimaryKey(20L)).thenReturn(null);

        CustomizeException ex = assertThrows(CustomizeException.class,
                () -> commentService.insert(comment, commentator(3000L, "bob")));
        assertEquals(CustomizeErrorCode.QUESTION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void listByTargetId_whenNoComments_returnsEmpty() {
        when(commentMapper.selectByExample(any())).thenReturn(new ArrayList<>());

        List<CommentDTO> result = commentService.listByTargetId(1L, CommentTypeEnum.QUESTION);

        assertTrue(result.isEmpty());
        verify(userMapper, never()).selectByExample(any());
    }

    @Test
    void listByTargetId_whenComments_mapsEachToDtoWithUser() {
        Comment c1 = new Comment();
        c1.setId(1L);
        c1.setCommentator(100L);
        Comment c2 = new Comment();
        c2.setId(2L);
        c2.setCommentator(200L);

        User u1 = commentator(100L, "u1");
        User u2 = commentator(200L, "u2");

        when(commentMapper.selectByExample(any())).thenReturn(Arrays.asList(c1, c2));
        when(userMapper.selectByExample(any())).thenReturn(Arrays.asList(u1, u2));

        List<CommentDTO> result = commentService.listByTargetId(1L, CommentTypeEnum.QUESTION);

        assertEquals(2, result.size());
        for (CommentDTO dto : result) {
            assertTrue(dto.getUser() != null);
        }
    }
}
