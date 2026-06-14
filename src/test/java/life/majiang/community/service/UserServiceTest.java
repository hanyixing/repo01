package life.majiang.community.service;

import life.majiang.community.mapper.UserMapper;
import life.majiang.community.model.User;
import life.majiang.community.model.UserExample;
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
public class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User newUser;
    private User existingUser;

    @BeforeEach
    void setUp() {
        newUser = new User();
        newUser.setAccountId("github_12345");
        newUser.setType("github");
        newUser.setName("testuser");
        newUser.setAvatarUrl("http://example.com/avatar.png");
        newUser.setToken("token_abc");

        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setAccountId("github_12345");
        existingUser.setType("github");
        existingUser.setName("oldname");
        existingUser.setAvatarUrl("http://example.com/old-avatar.png");
        existingUser.setToken("old_token");
        existingUser.setGmtCreate(1000L);
        existingUser.setGmtModified(2000L);
    }

    @Test
    void testCreateOrUpdate_newUser_insertsUser() {
        when(userMapper.selectByExample(any(UserExample.class))).thenReturn(new ArrayList<>());

        userService.createOrUpdate(newUser);

        verify(userMapper).insert(argThat(u -> {
            assertNotNull(u.getGmtCreate());
            assertNotNull(u.getGmtModified());
            assertEquals(u.getGmtCreate(), u.getGmtModified());
            assertEquals("github_12345", u.getAccountId());
            assertEquals("github", u.getType());
            assertEquals("testuser", u.getName());
            assertEquals("http://example.com/avatar.png", u.getAvatarUrl());
            assertEquals("token_abc", u.getToken());
            return true;
        }));
        verify(userMapper, never()).updateByExampleSelective(any(User.class), any(UserExample.class));
    }

    @Test
    void testCreateOrUpdate_existingUser_updatesFields() {
        List<User> users = Collections.singletonList(existingUser);
        when(userMapper.selectByExample(any(UserExample.class))).thenReturn(users);

        userService.createOrUpdate(newUser);

        verify(userMapper, never()).insert(any(User.class));
        verify(userMapper).updateByExampleSelective(
                argThat(u -> {
                    assertNotNull(u.getGmtModified());
                    assertEquals("http://example.com/avatar.png", u.getAvatarUrl());
                    assertEquals("testuser", u.getName());
                    assertEquals("token_abc", u.getToken());
                    return true;
                }),
                any(UserExample.class));
    }

    @Test
    void testCreateOrUpdate_existingUser_preservesId() {
        List<User> users = Collections.singletonList(existingUser);
        when(userMapper.selectByExample(any(UserExample.class))).thenReturn(users);

        userService.createOrUpdate(newUser);

        verify(userMapper).updateByExampleSelective(any(User.class), any(UserExample.class));
    }

    @Test
    void testCreateOrUpdate_newUser_setsTimestamps() {
        when(userMapper.selectByExample(any(UserExample.class))).thenReturn(new ArrayList<>());

        long beforeTime = System.currentTimeMillis();
        userService.createOrUpdate(newUser);
        long afterTime = System.currentTimeMillis();

        verify(userMapper).insert(argThat(u -> {
            assertTrue(u.getGmtCreate() >= beforeTime && u.getGmtCreate() <= afterTime);
            assertEquals(u.getGmtCreate(), u.getGmtModified());
            return true;
        }));
    }

    @Test
    void testCreateOrUpdate_existingUser_updatesGmtModified() {
        List<User> users = Collections.singletonList(existingUser);
        when(userMapper.selectByExample(any(UserExample.class))).thenReturn(users);

        long beforeTime = System.currentTimeMillis();
        userService.createOrUpdate(newUser);
        long afterTime = System.currentTimeMillis();

        verify(userMapper).updateByExampleSelective(
                argThat(u -> {
                    assertTrue(u.getGmtModified() >= beforeTime && u.getGmtModified() <= afterTime);
                    return true;
                }),
                any(UserExample.class));
    }

    @Test
    void testCreateOrUpdate_differentType_createsNewUser() {
        User giteeUser = new User();
        giteeUser.setAccountId("github_12345");
        giteeUser.setType("gitee");
        giteeUser.setName("giteeuser");
        giteeUser.setToken("gitee_token");

        when(userMapper.selectByExample(any(UserExample.class))).thenReturn(new ArrayList<>());

        userService.createOrUpdate(giteeUser);

        verify(userMapper).insert(argThat(u -> {
            assertEquals("gitee", u.getType());
            assertEquals("giteeuser", u.getName());
            return true;
        }));
    }
}
