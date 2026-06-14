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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

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

    private Question buildQuestion(Long id, Long creator) {
        Question question = new Question();
        question.setId(id);
        question.setCreator(creator);
        question.setTitle("title-" + id);
        question.setDescription("a long description that should be cleared in list");
        question.setTag("java");
        return question;
    }

    @Test
    void list_withSearch_returnsPaginatedDtosWithUserAndClearedDescription() {
        Question question = buildQuestion(1L, 1000L);
        User author = new User();
        author.setId(1000L);
        author.setName("alice");

        when(questionExtMapper.countBySearch(any(QuestionQueryDTO.class))).thenReturn(10);
        when(questionExtMapper.selectBySearch(any(QuestionQueryDTO.class)))
                .thenReturn(Collections.singletonList(question));
        when(userMapper.selectByPrimaryKey(1000L)).thenReturn(author);
        when(questionCache.getStickies()).thenReturn(Collections.emptyList());

        PaginationDTO<QuestionDTO> result = questionService.list("java spring", "java", "hot", 1, 5);

        assertNotNull(result);
        assertEquals(2, result.getTotalPage().intValue());
        assertEquals(1, result.getPage().intValue());
        assertEquals(1, result.getData().size());
        QuestionDTO dto = result.getData().get(0);
        assertEquals("alice", dto.getUser().getName());
        assertEquals("", dto.getDescription());
    }

    @Test
    void list_withBlankSearch_prependsStickies() {
        QuestionDTO sticky = new QuestionDTO();
        sticky.setId(99L);

        when(questionExtMapper.countBySearch(any(QuestionQueryDTO.class))).thenReturn(5);
        when(questionExtMapper.selectBySearch(any(QuestionQueryDTO.class))).thenReturn(new ArrayList<>());
        when(questionCache.getStickies()).thenReturn(Collections.singletonList(sticky));

        PaginationDTO<QuestionDTO> result = questionService.list(null, null, null, 1, 5);

        assertEquals(1, result.getTotalPage().intValue());
        assertEquals(1, result.getData().size());
        assertEquals(99L, result.getData().get(0).getId().longValue());
    }

    @Test
    void getById_whenFound_returnsDtoWithUser() {
        Question question = buildQuestion(1L, 2L);
        User author = new User();
        author.setId(2L);
        author.setName("bob");
        when(questionMapper.selectByPrimaryKey(1L)).thenReturn(question);
        when(userMapper.selectByPrimaryKey(2L)).thenReturn(author);

        QuestionDTO dto = questionService.getById(1L);

        assertEquals(1L, dto.getId().longValue());
        assertEquals("bob", dto.getUser().getName());
    }

    @Test
    void getById_whenNotFound_throwsQuestionNotFound() {
        when(questionMapper.selectByPrimaryKey(99L)).thenReturn(null);

        CustomizeException ex = assertThrows(CustomizeException.class, () -> questionService.getById(99L));
        assertEquals(CustomizeErrorCode.QUESTION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void createOrUpdate_whenIdNull_setsDefaultsAndInserts() {
        Question question = new Question();
        question.setCreator(1L);
        question.setTitle("new");
        question.setDescription("desc");
        question.setTag("java");

        questionService.createOrUpdate(question);

        verify(questionMapper).insert(question);
        verify(questionMapper, never()).updateByExampleSelective(any(), any());
        assertEquals(0, question.getViewCount().intValue());
        assertEquals(0, question.getLikeCount().intValue());
        assertEquals(0, question.getCommentCount().intValue());
        assertEquals(0, question.getSticky().intValue());
        assertNotNull(question.getGmtCreate());
        assertEquals(question.getGmtCreate(), question.getGmtModified());
    }

    @Test
    void createOrUpdate_whenExistsAndOwner_updates() {
        Question question = buildQuestion(5L, 1L);
        Question dbQuestion = buildQuestion(5L, 1L);
        when(questionMapper.selectByPrimaryKey(5L)).thenReturn(dbQuestion);
        when(questionMapper.updateByExampleSelective(any(Question.class), any(QuestionExample.class))).thenReturn(1);

        questionService.createOrUpdate(question);

        verify(questionMapper).updateByExampleSelective(any(Question.class), any(QuestionExample.class));
        verify(questionMapper, never()).insert(any());
    }

    @Test
    void createOrUpdate_whenUpdateTargetMissing_throwsQuestionNotFound() {
        Question question = buildQuestion(5L, 1L);
        when(questionMapper.selectByPrimaryKey(5L)).thenReturn(null);

        CustomizeException ex = assertThrows(CustomizeException.class, () -> questionService.createOrUpdate(question));
        assertEquals(CustomizeErrorCode.QUESTION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void createOrUpdate_whenDifferentCreator_throwsInvalidOperation() {
        Question question = buildQuestion(5L, 2L);
        Question dbQuestion = buildQuestion(5L, 1L);
        when(questionMapper.selectByPrimaryKey(5L)).thenReturn(dbQuestion);

        CustomizeException ex = assertThrows(CustomizeException.class, () -> questionService.createOrUpdate(question));
        assertEquals(CustomizeErrorCode.INVALID_OPERATION.getCode(), ex.getCode());
    }

    @Test
    void createOrUpdate_whenUpdateAffectsNoRow_throwsQuestionNotFound() {
        Question question = buildQuestion(5L, 1L);
        Question dbQuestion = buildQuestion(5L, 1L);
        when(questionMapper.selectByPrimaryKey(5L)).thenReturn(dbQuestion);
        when(questionMapper.updateByExampleSelective(any(Question.class), any(QuestionExample.class))).thenReturn(0);

        CustomizeException ex = assertThrows(CustomizeException.class, () -> questionService.createOrUpdate(question));
        assertEquals(CustomizeErrorCode.QUESTION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void incView_buildsQuestionAndDelegatesToExtMapper() {
        questionService.incView(7L);

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionExtMapper).incView(captor.capture());
        assertEquals(7L, captor.getValue().getId().longValue());
        assertEquals(1, captor.getValue().getViewCount().intValue());
    }

    @Test
    void selectRelated_whenTagBlank_returnsEmptyWithoutQuery() {
        QuestionDTO query = new QuestionDTO();
        query.setTag(null);

        List<QuestionDTO> result = questionService.selectRelated(query);

        assertTrue(result.isEmpty());
        verify(questionExtMapper, never()).selectRelated(any());
    }

    @Test
    void selectRelated_whenTagPresent_mapsResultsToDtos() {
        QuestionDTO query = new QuestionDTO();
        query.setId(1L);
        query.setTag("java,spring");
        when(questionExtMapper.selectRelated(any(Question.class)))
                .thenReturn(java.util.Arrays.asList(buildQuestion(2L, 1L), buildQuestion(3L, 1L)));

        List<QuestionDTO> result = questionService.selectRelated(query);

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getId().longValue());
        assertEquals(3L, result.get(1).getId().longValue());
        verify(questionExtMapper, times(1)).selectRelated(any(Question.class));
    }
}
