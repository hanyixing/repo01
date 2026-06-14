package life.majiang.community.provider;

import life.majiang.community.dto.AccessTokenDTO;
import life.majiang.community.provider.dto.GiteeUser;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class GiteeProviderTest {

    private final GiteeProvider giteeProvider = new GiteeProvider();

    private MockedConstruction.MockInitializer<OkHttpClient> respondWith(String responseBody) {
        return (mock, context) -> {
            Call call = mock(Call.class);
            Response response = mock(Response.class);
            ResponseBody body = mock(ResponseBody.class);
            when(body.string()).thenReturn(responseBody);
            when(response.body()).thenReturn(body);
            when(call.execute()).thenReturn(response);
            when(mock.newCall(any(Request.class))).thenReturn(call);
        };
    }

    private MockedConstruction.MockInitializer<OkHttpClient> failWith(IOException exception) {
        return (mock, context) -> {
            Call call = mock(Call.class);
            when(call.execute()).thenThrow(exception);
            when(mock.newCall(any(Request.class))).thenReturn(call);
        };
    }

    @Test
    void getAccessToken_success_extractsAccessTokenFromJson() {
        AccessTokenDTO dto = new AccessTokenDTO();
        dto.setCode("code-456");

        try (MockedConstruction<OkHttpClient> ignored = mockConstruction(OkHttpClient.class,
                respondWith("{\"access_token\":\"gitee_tok\",\"token_type\":\"bearer\"}"))) {
            String token = giteeProvider.getAccessToken(dto);
            assertEquals("gitee_tok", token);
        }
    }

    @Test
    void getAccessToken_whenRequestFails_returnsNull() {
        AccessTokenDTO dto = new AccessTokenDTO();
        dto.setCode("code-456");

        try (MockedConstruction<OkHttpClient> ignored = mockConstruction(OkHttpClient.class,
                failWith(new IOException("network down")))) {
            assertNull(giteeProvider.getAccessToken(dto));
        }
    }

    @Test
    void getUser_success_deserializesGiteeUser() {
        try (MockedConstruction<OkHttpClient> ignored = mockConstruction(OkHttpClient.class,
                respondWith("{\"name\":\"gitee_user\",\"id\":456,\"bio\":\"b\",\"avatarUrl\":\"http://y/b.png\"}"))) {
            GiteeUser user = giteeProvider.getUser("token-abc");
            assertEquals("gitee_user", user.getName());
            assertEquals(456L, user.getId().longValue());
            assertEquals("b", user.getBio());
            assertEquals("http://y/b.png", user.getAvatarUrl());
        }
    }

    @Test
    void getUser_whenRequestFails_returnsNull() {
        try (MockedConstruction<OkHttpClient> ignored = mockConstruction(OkHttpClient.class,
                failWith(new IOException("boom")))) {
            assertNull(giteeProvider.getUser("token-abc"));
        }
    }
}
