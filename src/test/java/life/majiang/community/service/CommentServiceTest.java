package life.majiang.community.service;

import life.majiang.community.dto.CommentDTO;
import life.majiang.community.enums.CommentTypeEnum;
import life.majiang.community.enums.NotificationTypeEnum;
import life.majiang.community.exception.CustomizeErrorCode;
import life.majiang.community.exception.CustomizeException;
import life.majiang.community.mapper.*;
import life.majiang.community.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

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

    private User commentator;
    private Question question;
    private Comment parentComment;

    @BeforeEach
    void setUp() {
        commentator = new User();
        commentator.setId(1L);
        commentator.setName("commentator");

        question = new Question();
        question.setId(100L);
        question.setTitle("Test Question");
        question.setCreator(2L);
        question.setCommentCount(0);

        parentComment = new Comment();
        parentComment.setId(50L);
        parentComment.setParentId(100L);
        parentComment.setCommentator(3L);
        parentComment.setContent("Parent comment content");
        parentComment.setType(CommentTypeEnum.QUESTION.getType());
    }

    // ==================== insert - reply to question ====================

    @Test
    void testInsert_replyToQuestion_success() {
        Comment comment = new Comment();
        comment.setParentId(100L);
        comment.setType(CommentTypeEnum.QUESTION.getType());
        comment.setContent("Great question!");
        comment.setCommentator(1L);

        when(questionMapper.selectByPrimaryKey(100L)).thenReturn(question);

        commentService.insert(comment, commentator);

        verify(commentMapper).insert(argThat(c -> {
            assertEquals(0, c.getCommentCount().intValue());
            return true;
        }));
        verify(questionExtMapper).incCommentCount(any(Question.class));
        verify(notificationMapper).insert(argThat(n -> {
            assertEquals((Object) NotificationTypeEnum.REPLY_QUESTION.getType(), (Object) n.getType());
            assertEquals(2L, n.getReceiver().longValue());
            assertEquals(1L, n.getNotifier().longValue());
            assertEquals("commentator", n.getNotifierName());
            assertEquals("Test Question", n.getOuterTitle());
            assertEquals(100L, n.getOuterid().longValue());
            return true;
        }));
    }

    @Test
    void testInsert_replyToQuestion_selfReply_noNotification() {
        User selfUser = new User();
        selfUser.setId(2L);
        selfUser.setName("selfuser");

        Comment comment = new Comment();
        comment.setParentId(100L);
        comment.setType(CommentTypeEnum.QUESTION.getType());
        comment.setContent("My own reply");
        comment.setCommentator(2L);

        when(questionMapper.selectByPrimaryKey(100L)).thenReturn(question);

        commentService.insert(comment, selfUser);

        verify(commentMapper).insert(any(Comment.class));
        verify(questionExtMapper).incCommentCount(any(Question.class));
        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    void testInsert_replyToQuestion_questionNotFound_throwsException() {
        Comment comment = new Comment();
        comment.setParentId(999L);
        comment.setType(CommentTypeEnum.QUESTION.getType());
        comment.setCommentator(1L);

        when(questionMapper.selectByPrimaryKey(999L)).thenReturn(null);

        CustomizeException exception = assertThrows(CustomizeException.class,
                () -> commentService.insert(comment, commentator));

        assertEquals(CustomizeErrorCode.QUESTION_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== insert - reply to comment ====================

    @Test
    void testInsert_replyToComment_success() {
        Comment comment = new Comment();
        comment.setParentId(50L);
        comment.setType(CommentTypeEnum.COMMENT.getType());
        comment.setContent("Reply to comment");
        comment.setCommentator(1L);

        when(commentMapper.selectByPrimaryKey(50L)).thenReturn(parentComment);
        when(questionMapper.selectByPrimaryKey(100L)).thenReturn(question);

        commentService.insert(comment, commentator);

        verify(commentMapper).insert(comment);
        verify(commentExtMapper).incCommentCount(argThat(c -> {
            assertEquals(50L, c.getId().longValue());
            assertEquals(1, c.getCommentCount().intValue());
            return true;
        }));
        verify(notificationMapper).insert(argThat(n -> {
            assertEquals((Object) NotificationTypeEnum.REPLY_COMMENT.getType(), (Object) n.getType());
            assertEquals(3L, n.getReceiver().longValue());
            return true;
        }));
    }

    @Test
    void testInsert_replyToComment_parentCommentNotFound_throwsException() {
        Comment comment = new Comment();
        comment.setParentId(999L);
        comment.setType(CommentTypeEnum.COMMENT.getType());
        comment.setCommentator(1L);

        when(commentMapper.selectByPrimaryKey(999L)).thenReturn(null);

        CustomizeException exception = assertThrows(CustomizeException.class,
                () -> commentService.insert(comment, commentator));

        assertEquals(CustomizeErrorCode.COMMENT_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void testInsert_replyToComment_questionNotFound_throwsException() {
        Comment comment = new Comment();
        comment.setParentId(50L);
        comment.setType(CommentTypeEnum.COMMENT.getType());
        comment.setCommentator(1L);

        when(commentMapper.selectByPrimaryKey(50L)).thenReturn(parentComment);
        when(questionMapper.selectByPrimaryKey(100L)).thenReturn(null);

        CustomizeException exception = assertThrows(CustomizeException.class,
                () -> commentService.insert(comment, commentator));

        assertEquals(CustomizeErrorCode.QUESTION_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== insert - parameter validation ====================

    @Test
    void testInsert_nullParentId_throwsException() {
        Comment comment = new Comment();
        comment.setParentId(null);
        comment.setType(CommentTypeEnum.QUESTION.getType());
        comment.setCommentator(1L);

        CustomizeException exception = assertThrows(CustomizeException.class,
                () -> commentService.insert(comment, commentator));

        assertEquals(CustomizeErrorCode.TARGET_PARAM_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void testInsert_zeroParentId_throwsException() {
        Comment comment = new Comment();
        comment.setParentId(0L);
        comment.setType(CommentTypeEnum.QUESTION.getType());
        comment.setCommentator(1L);

        CustomizeException exception = assertThrows(CustomizeException.class,
                () -> commentService.insert(comment, commentator));

        assertEquals(CustomizeErrorCode.TARGET_PARAM_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void testInsert_nullType_throwsException() {
        Comment comment = new Comment();
        comment.setParentId(100L);
        comment.setType(null);
        comment.setCommentator(1L);

        CustomizeException exception = assertThrows(CustomizeException.class,
                () -> commentService.insert(comment, commentator));

        assertEquals(CustomizeErrorCode.TYPE_PARAM_WRONG.getCode(), exception.getCode());
    }

    @Test
    void testInsert_invalidType_throwsException() {
        Comment comment = new Comment();
        comment.setParentId(100L);
        comment.setType(99);
        comment.setCommentator(1L);

        CustomizeException exception = assertThrows(CustomizeException.class,
                () -> commentService.insert(comment, commentator));

        assertEquals(CustomizeErrorCode.TYPE_PARAM_WRONG.getCode(), exception.getCode());
    }

    // ==================== listByTargetId ====================

    @Test
    void testListByTargetId_withComments_returnsDTOsWithUsers() {
        User user1 = new User();
        user1.setId(1L);
        user1.setName("user1");

        User user2 = new User();
        user2.setId(3L);
        user2.setName("user3");

        Comment comment1 = new Comment();
        comment1.setId(1L);
        comment1.setParentId(100L);
        comment1.setCommentator(1L);
        comment1.setContent("Comment 1");
        comment1.setType(CommentTypeEnum.QUESTION.getType());
        comment1.setGmtCreate(System.currentTimeMillis());

        Comment comment2 = new Comment();
        comment2.setId(2L);
        comment2.setParentId(100L);
        comment2.setCommentator(3L);
        comment2.setContent("Comment 2");
        comment2.setType(CommentTypeEnum.QUESTION.getType());
        comment2.setGmtCreate(System.currentTimeMillis());

        List<Comment> comments = new ArrayList<>();
        comments.add(comment1);
        comments.add(comment2);

        when(commentMapper.selectByExample(any(CommentExample.class))).thenReturn(comments);

        List<User> users = new ArrayList<>();
        users.add(user1);
        users.add(user2);
        when(userMapper.selectByExample(any(UserExample.class))).thenReturn(users);

        List<CommentDTO> result = commentService.listByTargetId(100L, CommentTypeEnum.QUESTION);

        assertEquals(2, result.size());
        assertEquals("Comment 1", result.get(0).getContent());
        assertEquals("user1", result.get(0).getUser().getName());
        assertEquals("Comment 2", result.get(1).getContent());
        assertEquals("user3", result.get(1).getUser().getName());
    }

    @Test
    void testListByTargetId_noComments_returnsEmptyList() {
        when(commentMapper.selectByExample(any(CommentExample.class))).thenReturn(new ArrayList<>());

        List<CommentDTO> result = commentService.listByTargetId(100L, CommentTypeEnum.QUESTION);

        assertTrue(result.isEmpty());
        verify(userMapper, never()).selectByExample(any(UserExample.class));
    }

    @Test
    void testListByTargetId_forCommentType_queriesCorrectly() {
        Comment replyComment = new Comment();
        replyComment.setId(10L);
        replyComment.setParentId(50L);
        replyComment.setCommentator(1L);
        replyComment.setContent("Reply");
        replyComment.setType(CommentTypeEnum.COMMENT.getType());

        User user = new User();
        user.setId(1L);
        user.setName("user1");

        when(commentMapper.selectByExample(any(CommentExample.class)))
                .thenReturn(Collections.singletonList(replyComment));
        when(userMapper.selectByExample(any(UserExample.class)))
                .thenReturn(Collections.singletonList(user));

        List<CommentDTO> result = commentService.listByTargetId(50L, CommentTypeEnum.COMMENT);

        assertEquals(1, result.size());
        assertEquals("Reply", result.get(0).getContent());
    }
}
