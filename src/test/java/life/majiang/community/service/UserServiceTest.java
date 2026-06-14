package life.majiang.community.service;

import life.majiang.community.mapper.UserMapper;
import life.majiang.community.model.User;
import life.majiang.community.model.UserExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User newUser() {
        User user = new User();
        user.setAccountId("account-1");
        user.setType("github");
        user.setName("alice");
        user.setToken("token-1");
        user.setAvatarUrl("http://avatar/a.png");
        return user;
    }

    @Test
    void createOrUpdate_whenUserNotExists_inserts() {
        User user = newUser();
        when(userMapper.selectByExample(any())).thenReturn(Collections.emptyList());

        userService.createOrUpdate(user);

        verify(userMapper).insert(user);
        verify(userMapper, never()).updateByExampleSelective(any(), any());
        assertNotNull(user.getGmtCreate());
        assertEquals(user.getGmtCreate(), user.getGmtModified());
    }

    @Test
    void createOrUpdate_whenUserExists_updates() {
        User user = newUser();
        User dbUser = newUser();
        dbUser.setId(42L);
        when(userMapper.selectByExample(any())).thenReturn(Collections.singletonList(dbUser));

        userService.createOrUpdate(user);

        verify(userMapper).updateByExampleSelective(any(User.class), any(UserExample.class));
        verify(userMapper, never()).insert(any());
    }
}
