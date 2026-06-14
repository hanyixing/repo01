package life.majiang.community.provider;

import life.majiang.community.dto.AccessTokenDTO;
import life.majiang.community.provider.dto.GithubUser;
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

class GithubProviderTest {

    private final GithubProvider githubProvider = new GithubProvider();

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
    void getAccessToken_success_parsesTokenFromResponse() {
        AccessTokenDTO dto = new AccessTokenDTO();
        dto.setCode("code-123");

        try (MockedConstruction<OkHttpClient> ignored = mockConstruction(OkHttpClient.class,
                respondWith("access_token=gho_abc123&scope=user&token_type=bearer"))) {
            String token = githubProvider.getAccessToken(dto);
            assertEquals("gho_abc123", token);
        }
    }

    @Test
    void getAccessToken_whenRequestFails_returnsNull() {
        AccessTokenDTO dto = new AccessTokenDTO();
        dto.setCode("code-123");

        try (MockedConstruction<OkHttpClient> ignored = mockConstruction(OkHttpClient.class,
                failWith(new IOException("network down")))) {
            assertNull(githubProvider.getAccessToken(dto));
        }
    }

    @Test
    void getUser_success_deserializesGithubUser() {
        try (MockedConstruction<OkHttpClient> ignored = mockConstruction(OkHttpClient.class,
                respondWith("{\"name\":\"alice\",\"id\":123,\"bio\":\"hello\",\"avatarUrl\":\"http://x/a.png\"}"))) {
            GithubUser user = githubProvider.getUser("token-xyz");
            assertEquals("alice", user.getName());
            assertEquals(123L, user.getId().longValue());
            assertEquals("hello", user.getBio());
            assertEquals("http://x/a.png", user.getAvatarUrl());
        }
    }

    @Test
    void getUser_whenRequestFails_returnsNull() {
        try (MockedConstruction<OkHttpClient> ignored = mockConstruction(OkHttpClient.class,
                failWith(new IOException("boom")))) {
            assertNull(githubProvider.getUser("token-xyz"));
        }
    }
}
