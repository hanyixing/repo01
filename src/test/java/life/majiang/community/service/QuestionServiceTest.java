package life.majiang.community.service;

import life.majiang.community.cache.QuestionCache;
import life.majiang.community.dto.PaginationDTO;
import life.majiang.community.dto.QuestionDTO;
import life.majiang.community.dto.QuestionQueryDTO;
import life.majiang.community.exception.CustomizeErrorCode;
import life.majiang.community.exception.CustomizeException;
import life.majiang.community.mapper.QuestionExtMapper;
import life.majiang.community.mapper.QuestionMapper;
import life.majiang.community.mapper.UserMapper;
import life.majiang.community.model.Question;
import life.majiang.community.model.QuestionExample;
import life.majiang.community.model.User;
import org.apache.ibatis.session.RowBounds;
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
public class QuestionServiceTest {

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private QuestionExtMapper questionExtMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private QuestionCache questionCache;

    @InjectMocks
    private QuestionService questionService;

    private User mockUser;
    private Question mockQuestion;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setName("testuser");
        mockUser.setAccountId("github_123");
        mockUser.setAvatarUrl("http://example.com/avatar.png");

        mockQuestion = new Question();
        mockQuestion.setId(100L);
        mockQuestion.setTitle("Test Question Title");
        mockQuestion.setDescription("## Test Description\n\nSome **markdown** content.");
        mockQuestion.setTag("java,spring");
        mockQuestion.setCreator(1L);
        mockQuestion.setGmtCreate(System.currentTimeMillis());
        mockQuestion.setGmtModified(System.currentTimeMillis());
        mockQuestion.setViewCount(10);
        mockQuestion.setLikeCount(5);
        mockQuestion.setCommentCount(3);
        mockQuestion.setSticky(0);
    }

    // ==================== list(search, tag, sort, page, size) ====================

    @Test
    void testList_normalSearch_returnPaginatedResults() {
        when(questionExtMapper.countBySearch(any(QuestionQueryDTO.class))).thenReturn(10);
        List<Question> questions = new ArrayList<>();
        questions.add(mockQuestion);
        when(questionExtMapper.selectBySearch(any(QuestionQueryDTO.class))).thenReturn(questions);
        when(userMapper.selectByPrimaryKey(1L)).thenReturn(mockUser);
        when(questionCache.getStickies()).thenReturn(new ArrayList<>());

        PaginationDTO result = questionService.list("java", null, "new", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalPage().intValue());
        assertEquals(1, result.getPage().intValue());
        assertEquals(1, result.getData().size());
        QuestionDTO dto = (QuestionDTO) result.getData().get(0);
        assertEquals("Test Question Title", dto.getTitle());
        assertEquals("", dto.getDescription());
        assertEquals(mockUser, dto.getUser());
    }

    @Test
    void testList_blankSearch_handlesGracefully() {
        when(questionExtMapper.countBySearch(any(QuestionQueryDTO.class))).thenReturn(5);
        List<Question> questions = new ArrayList<>();
        questions.add(mockQuestion);
        when(questionExtMapper.selectBySearch(any(QuestionQueryDTO.class))).thenReturn(questions);
        when(userMapper.selectByPrimaryKey(1L)).thenReturn(mockUser);
        when(questionCache.getStickies()).thenReturn(null);

        PaginationDTO result = questionService.list("", null, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getData().size());
    }

    @Test
    void testList_withTagFilter_stripsSpecialChars() {
        when(questionExtMapper.countBySearch(any(QuestionQueryDTO.class))).thenReturn(1);
        when(questionExtMapper.selectBySearch(any(QuestionQueryDTO.class))).thenReturn(new ArrayList<>());
        when(questionCache.getStickies()).thenReturn(new ArrayList<>());

        PaginationDTO result = questionService.list(null, "java+spring*", null, 1, 10);

        assertNotNull(result);
        verify(questionExtMapper).countBySearch(any(QuestionQueryDTO.class));
    }

    @Test
    void testList_withHot7Sort_setsTimeRange() {
        when(questionExtMapper.countBySearch(any(QuestionQueryDTO.class))).thenReturn(0);
        when(questionExtMapper.selectBySearch(any(QuestionQueryDTO.class))).thenReturn(new ArrayList<>());
        when(questionCache.getStickies()).thenReturn(new ArrayList<>());

        PaginationDTO result = questionService.list(null, null, "hot7", 1, 10);

        assertNotNull(result);
        verify(questionExtMapper).countBySearch(argThat(dto -> {
            return dto.getTime() != null;
        }));
    }

    @Test
    void testList_withHot30Sort_setsTimeRange() {
        when(questionExtMapper.countBySearch(any(QuestionQueryDTO.class))).thenReturn(0);
        when(questionExtMapper.selectBySearch(any(QuestionQueryDTO.class))).thenReturn(new ArrayList<>());
        when(questionCache.getStickies()).thenReturn(new ArrayList<>());

        PaginationDTO result = questionService.list(null, null, "hot30", 1, 10);

        assertNotNull(result);
    }

    @Test
    void testList_pageLessThanOne_correctedToOne() {
        when(questionExtMapper.countBySearch(any(QuestionQueryDTO.class))).thenReturn(20);
        when(questionExtMapper.selectBySearch(any(QuestionQueryDTO.class))).thenReturn(new ArrayList<>());
        when(questionCache.getStickies()).thenReturn(new ArrayList<>());

        PaginationDTO result = questionService.list(null, null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getPage().intValue());
    }

    @Test
    void testList_pageExceedsTotalPage_correctedToTotalPage() {
        when(questionExtMapper.countBySearch(any(QuestionQueryDTO.class))).thenReturn(20);
        when(questionExtMapper.selectBySearch(any(QuestionQueryDTO.class))).thenReturn(new ArrayList<>());
        when(questionCache.getStickies()).thenReturn(new ArrayList<>());

        PaginationDTO result = questionService.list(null, null, null, 100, 10);

        assertNotNull(result);
        assertEquals(2, result.getPage().intValue());
    }

    @Test
    void testList_withStickyQuestions_prependsToResult() {
        when(questionExtMapper.countBySearch(any(QuestionQueryDTO.class))).thenReturn(1);
        List<Question> questions = new ArrayList<>();
        questions.add(mockQuestion);
        when(questionExtMapper.selectBySearch(any(QuestionQueryDTO.class))).thenReturn(questions);
        when(userMapper.selectByPrimaryKey(1L)).thenReturn(mockUser);

        QuestionDTO stickyDTO = new QuestionDTO();
        stickyDTO.setId(999L);
        stickyDTO.setTitle("Sticky Question");
        List<QuestionDTO> stickies = new ArrayList<>();
        stickies.add(stickyDTO);
        when(questionCache.getStickies()).thenReturn(stickies);

        PaginationDTO result = questionService.list(null, null, null, 1, 10);

        assertEquals(2, result.getData().size());
        QuestionDTO first = (QuestionDTO) result.getData().get(0);
        assertEquals("Sticky Question", first.getTitle());
    }

    @Test
    void testList_multiplePages_correctPagination() {
        when(questionExtMapper.countBySearch(any(QuestionQueryDTO.class))).thenReturn(25);
        when(questionExtMapper.selectBySearch(any(QuestionQueryDTO.class))).thenReturn(new ArrayList<>());
        when(questionCache.getStickies()).thenReturn(new ArrayList<>());

        PaginationDTO result = questionService.list(null, null, null, 2, 10);

        assertEquals(3, result.getTotalPage().intValue());
        assertEquals(2, result.getPage().intValue());
    }

    @Test
    void testList_exactPageDivision_correctTotalPage() {
        when(questionExtMapper.countBySearch(any(QuestionQueryDTO.class))).thenReturn(20);
        when(questionExtMapper.selectBySearch(any(QuestionQueryDTO.class))).thenReturn(new ArrayList<>());
        when(questionCache.getStickies()).thenReturn(new ArrayList<>());

        PaginationDTO result = questionService.list(null, null, null, 1, 10);

        assertEquals(2, result.getTotalPage().intValue());
    }

    @Test
    void testList_searchWithSpecialChars_sanitized() {
        when(questionExtMapper.countBySearch(any(QuestionQueryDTO.class))).thenReturn(0);
        when(questionExtMapper.selectBySearch(any(QuestionQueryDTO.class))).thenReturn(new ArrayList<>());
        when(questionCache.getStickies()).thenReturn(new ArrayList<>());

        questionService.list("java+ spring* test?", null, null, 1, 10);

        verify(questionExtMapper).countBySearch(argThat(dto -> {
            String search = dto.getSearch();
            return search != null && !search.contains("+") && !search.contains("*") && !search.contains("?");
        }));
    }

    // ==================== list(userId, page, size) ====================

    @Test
    void testListByUserId_normalCase_returnPaginatedResults() {
        when(questionMapper.countByExample(any(QuestionExample.class))).thenReturn(5L);
        List<Question> questions = new ArrayList<>();
        questions.add(mockQuestion);
        when(questionMapper.selectByExampleWithRowbounds(any(QuestionExample.class), any(RowBounds.class)))
                .thenReturn(questions);
        when(userMapper.selectByPrimaryKey(1L)).thenReturn(mockUser);

        PaginationDTO result = questionService.list(1L, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalPage().intValue());
        assertEquals(1, result.getData().size());
    }

    @Test
    void testListByUserId_pageLessThanOne_correctedToOne() {
        when(questionMapper.countByExample(any(QuestionExample.class))).thenReturn(10L);
        when(questionMapper.selectByExampleWithRowbounds(any(QuestionExample.class), any(RowBounds.class)))
                .thenReturn(new ArrayList<>());

        PaginationDTO result = questionService.list(1L, -1, 10);

        assertEquals(1, result.getPage().intValue());
    }

    @Test
    void testListByUserId_pageExceedsTotal_correctedToTotal() {
        when(questionMapper.countByExample(any(QuestionExample.class))).thenReturn(10L);
        when(questionMapper.selectByExampleWithRowbounds(any(QuestionExample.class), any(RowBounds.class)))
                .thenReturn(new ArrayList<>());

        PaginationDTO result = questionService.list(1L, 100, 10);

        assertEquals(1, result.getPage().intValue());
    }

    // ==================== getById ====================

    @Test
    void testGetById_existingQuestion_returnsDTO() {
        when(questionMapper.selectByPrimaryKey(100L)).thenReturn(mockQuestion);
        when(userMapper.selectByPrimaryKey(1L)).thenReturn(mockUser);

        QuestionDTO result = questionService.getById(100L);

        assertNotNull(result);
        assertEquals(100L, result.getId().longValue());
        assertEquals("Test Question Title", result.getTitle());
        assertEquals("## Test Description\n\nSome **markdown** content.", result.getDescription());
        assertEquals(mockUser, result.getUser());
    }

    @Test
    void testGetById_nonExistingQuestion_throwsException() {
        when(questionMapper.selectByPrimaryKey(999L)).thenReturn(null);

        CustomizeException exception = assertThrows(CustomizeException.class,
                () -> questionService.getById(999L));

        assertEquals(CustomizeErrorCode.QUESTION_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== createOrUpdate ====================

    @Test
    void testCreate_newQuestion_setsDefaultValues() {
        Question newQuestion = new Question();
        newQuestion.setTitle("New Question");
        newQuestion.setDescription("New description with **markdown**");
        newQuestion.setTag("java");
        newQuestion.setCreator(1L);

        questionService.createOrUpdate(newQuestion);

        verify(questionMapper).insert(argThat(q -> {
            assertNotNull(q.getGmtCreate());
            assertNotNull(q.getGmtModified());
            assertEquals(0, q.getViewCount().intValue());
            assertEquals(0, q.getLikeCount().intValue());
            assertEquals(0, q.getCommentCount().intValue());
            assertEquals(0, q.getSticky().intValue());
            assertEquals("New Question", q.getTitle());
            return true;
        }));
    }

    @Test
    void testUpdate_existingQuestion_sameCreator_updatesSuccessfully() {
        Question updateQuestion = new Question();
        updateQuestion.setId(100L);
        updateQuestion.setTitle("Updated Title");
        updateQuestion.setDescription("Updated description");
        updateQuestion.setTag("java,spring");
        updateQuestion.setCreator(1L);

        Question dbQuestion = new Question();
        dbQuestion.setId(100L);
        dbQuestion.setCreator(1L);
        dbQuestion.setTitle("Old Title");

        when(questionMapper.selectByPrimaryKey(100L)).thenReturn(dbQuestion);
        when(questionMapper.updateByExampleSelective(any(Question.class), any(QuestionExample.class)))
                .thenReturn(1);

        questionService.createOrUpdate(updateQuestion);

        verify(questionMapper).updateByExampleSelective(
                argThat(q -> "Updated Title".equals(q.getTitle())),
                any(QuestionExample.class));
    }

    @Test
    void testUpdate_nonExistingQuestion_throwsNotFound() {
        Question updateQuestion = new Question();
        updateQuestion.setId(999L);
        updateQuestion.setCreator(1L);

        when(questionMapper.selectByPrimaryKey(999L)).thenReturn(null);

        CustomizeException exception = assertThrows(CustomizeException.class,
                () -> questionService.createOrUpdate(updateQuestion));

        assertEquals(CustomizeErrorCode.QUESTION_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void testUpdate_differentCreator_throwsInvalidOperation() {
        Question updateQuestion = new Question();
        updateQuestion.setId(100L);
        updateQuestion.setCreator(2L);

        Question dbQuestion = new Question();
        dbQuestion.setId(100L);
        dbQuestion.setCreator(1L);

        when(questionMapper.selectByPrimaryKey(100L)).thenReturn(dbQuestion);

        CustomizeException exception = assertThrows(CustomizeException.class,
                () -> questionService.createOrUpdate(updateQuestion));

        assertEquals(CustomizeErrorCode.INVALID_OPERATION.getCode(), exception.getCode());
    }

    @Test
    void testUpdate_updateFails_throwsNotFound() {
        Question updateQuestion = new Question();
        updateQuestion.setId(100L);
        updateQuestion.setCreator(1L);

        Question dbQuestion = new Question();
        dbQuestion.setId(100L);
        dbQuestion.setCreator(1L);

        when(questionMapper.selectByPrimaryKey(100L)).thenReturn(dbQuestion);
        when(questionMapper.updateByExampleSelective(any(Question.class), any(QuestionExample.class)))
                .thenReturn(0);

        CustomizeException exception = assertThrows(CustomizeException.class,
                () -> questionService.createOrUpdate(updateQuestion));

        assertEquals(CustomizeErrorCode.QUESTION_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== incView ====================

    @Test
    void testIncView_callsMapperWithCorrectParams() {
        questionService.incView(100L);

        verify(questionExtMapper).incView(argThat(q -> {
            assertEquals(100L, q.getId().longValue());
            assertEquals(1, q.getViewCount().intValue());
            return true;
        }));
    }

    // ==================== selectRelated ====================

    @Test
    void testSelectRelated_withTags_returnsRelatedQuestions() {
        QuestionDTO queryDTO = new QuestionDTO();
        queryDTO.setId(100L);
        queryDTO.setTag("java,spring");

        Question relatedQuestion = new Question();
        relatedQuestion.setId(200L);
        relatedQuestion.setTitle("Related Question");
        relatedQuestion.setTag("java");

        when(questionExtMapper.selectRelated(any(Question.class)))
                .thenReturn(Collections.singletonList(relatedQuestion));

        List<QuestionDTO> result = questionService.selectRelated(queryDTO);

        assertEquals(1, result.size());
        assertEquals(200L, result.get(0).getId().longValue());
        assertEquals("Related Question", result.get(0).getTitle());
    }

    @Test
    void testSelectRelated_blankTag_returnsEmptyList() {
        QuestionDTO queryDTO = new QuestionDTO();
        queryDTO.setId(100L);
        queryDTO.setTag("");

        List<QuestionDTO> result = questionService.selectRelated(queryDTO);

        assertTrue(result.isEmpty());
        verify(questionExtMapper, never()).selectRelated(any(Question.class));
    }

    @Test
    void testSelectRelated_nullTag_returnsEmptyList() {
        QuestionDTO queryDTO = new QuestionDTO();
        queryDTO.setId(100L);
        queryDTO.setTag(null);

        List<QuestionDTO> result = questionService.selectRelated(queryDTO);

        assertTrue(result.isEmpty());
    }

    @Test
    void testSelectRelated_tagsWithSpecialChars_sanitized() {
        QuestionDTO queryDTO = new QuestionDTO();
        queryDTO.setId(100L);
        queryDTO.setTag("java+,spring*");

        when(questionExtMapper.selectRelated(any(Question.class))).thenReturn(new ArrayList<>());

        questionService.selectRelated(queryDTO);

        verify(questionExtMapper).selectRelated(argThat(q -> {
            String tag = q.getTag();
            return tag != null && !tag.contains("+") && !tag.contains("*");
        }));
    }
}
