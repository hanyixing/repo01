package life.majiang.community.provider;

import life.majiang.community.dto.AccessTokenDTO;
import life.majiang.community.provider.dto.GithubUser;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GithubProviderTest {

    private GithubProvider githubProvider;
    private AccessTokenDTO accessTokenDTO;

    @BeforeEach
    void setUp() {
        githubProvider = new GithubProvider();
        ReflectionTestUtils.setField(githubProvider, "clientId", "test_client_id");
        ReflectionTestUtils.setField(githubProvider, "clientSecret", "test_client_secret");
        ReflectionTestUtils.setField(githubProvider, "redirectUri", "http://localhost:8887/callback/github");

        accessTokenDTO = new AccessTokenDTO();
        accessTokenDTO.setCode("test_code");
        accessTokenDTO.setState("test_state");
    }

    // ==================== getAccessToken ====================

    @Test
    void testGetAccessToken_success() throws Exception {
        String responseBody = "access_token=gho_test_token_123&scope=user&token_type=bearer";

        try (MockedConstruction<OkHttpClient> mocked = mockConstruction(OkHttpClient.class,
                (mock, context) -> {
                    Call mockCall = mock(Call.class);
                    ResponseBody mockBody = mock(ResponseBody.class);
                    when(mockBody.string()).thenReturn(responseBody);
                    Response response = new Response.Builder()
                            .request(new Request.Builder().url("https://github.com/login/oauth/access_token").build())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(mockBody)
                            .build();
                    when(mock.newCall(any())).thenReturn(mockCall);
                    when(mockCall.execute()).thenReturn(response);
                })) {

            String token = githubProvider.getAccessToken(accessTokenDTO);

            assertEquals("gho_test_token_123", token);
            assertEquals(1, mocked.constructed().size());
        }
    }

    @Test
    void testGetAccessToken_setsClientFields() throws Exception {
        String responseBody = "access_token=test_token&scope=user";

        try (MockedConstruction<OkHttpClient> mocked = mockConstruction(OkHttpClient.class,
                (mock, context) -> {
                    Call mockCall = mock(Call.class);
                    ResponseBody mockBody = mock(ResponseBody.class);
                    when(mockBody.string()).thenReturn(responseBody);
                    Response response = new Response.Builder()
                            .request(new Request.Builder().url("https://github.com/login/oauth/access_token").build())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(mockBody)
                            .build();
                    when(mock.newCall(any())).thenReturn(mockCall);
                    when(mockCall.execute()).thenReturn(response);
                })) {

            githubProvider.getAccessToken(accessTokenDTO);

            assertEquals("test_client_id", accessTokenDTO.getClient_id());
            assertEquals("test_client_secret", accessTokenDTO.getClient_secret());
            assertEquals("http://localhost:8887/callback/github", accessTokenDTO.getRedirect_uri());
        }
    }

    @Test
    void testGetAccessToken_networkError_returnsNull() throws Exception {
        try (MockedConstruction<OkHttpClient> mocked = mockConstruction(OkHttpClient.class,
                (mock, context) -> {
                    Call mockCall = mock(Call.class);
                    when(mock.newCall(any())).thenReturn(mockCall);
                    when(mockCall.execute()).thenThrow(new IOException("Connection refused"));
                })) {

            String token = githubProvider.getAccessToken(accessTokenDTO);

            assertNull(token);
        }
    }

    @Test
    void testGetAccessToken_invalidResponseFormat_returnsNull() throws Exception {
        String responseBody = "invalid_response_no_equals_sign";

        try (MockedConstruction<OkHttpClient> mocked = mockConstruction(OkHttpClient.class,
                (mock, context) -> {
                    Call mockCall = mock(Call.class);
                    ResponseBody mockBody = mock(ResponseBody.class);
                    when(mockBody.string()).thenReturn(responseBody);
                    Response response = new Response.Builder()
                            .request(new Request.Builder().url("https://github.com/login/oauth/access_token").build())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(mockBody)
                            .build();
                    when(mock.newCall(any())).thenReturn(mockCall);
                    when(mockCall.execute()).thenReturn(response);
                })) {

            String token = githubProvider.getAccessToken(accessTokenDTO);

            assertNull(token);
        }
    }

    @Test
    void testGetAccessToken_errorResponse_stillParsesToken() throws Exception {
        String responseBody = "error=bad_verification_code&error_description=The+code+passed+is+incorrect";

        try (MockedConstruction<OkHttpClient> mocked = mockConstruction(OkHttpClient.class,
                (mock, context) -> {
                    Call mockCall = mock(Call.class);
                    ResponseBody mockBody = mock(ResponseBody.class);
                    when(mockBody.string()).thenReturn(responseBody);
                    Response response = new Response.Builder()
                            .request(new Request.Builder().url("https://github.com/login/oauth/access_token").build())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(mockBody)
                            .build();
                    when(mock.newCall(any())).thenReturn(mockCall);
                    when(mockCall.execute()).thenReturn(response);
                })) {

            String token = githubProvider.getAccessToken(accessTokenDTO);

            assertEquals("bad_verification_code", token);
        }
    }

    // ==================== getUser ====================

    @Test
    void testGetUser_success() throws Exception {
        String responseJson = "{\"id\":12345,\"name\":\"Test User\",\"bio\":\"A developer\",\"avatarUrl\":\"http://example.com/avatar.png\"}";

        try (MockedConstruction<OkHttpClient> mocked = mockConstruction(OkHttpClient.class,
                (mock, context) -> {
                    Call mockCall = mock(Call.class);
                    ResponseBody mockBody = mock(ResponseBody.class);
                    when(mockBody.string()).thenReturn(responseJson);
                    Response response = new Response.Builder()
                            .request(new Request.Builder().url("https://api.github.com/user").build())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(mockBody)
                            .build();
                    when(mock.newCall(any())).thenReturn(mockCall);
                    when(mockCall.execute()).thenReturn(response);
                })) {

            GithubUser user = githubProvider.getUser("test_token");

            assertNotNull(user);
            assertEquals(Long.valueOf(12345L), user.getId());
            assertEquals("Test User", user.getName());
            assertEquals("A developer", user.getBio());
        }
    }

    @Test
    void testGetUser_networkError_returnsNull() throws Exception {
        try (MockedConstruction<OkHttpClient> mocked = mockConstruction(OkHttpClient.class,
                (mock, context) -> {
                    Call mockCall = mock(Call.class);
                    when(mock.newCall(any())).thenReturn(mockCall);
                    when(mockCall.execute()).thenThrow(new IOException("Connection timeout"));
                })) {

            GithubUser user = githubProvider.getUser("test_token");

            assertNull(user);
        }
    }

    @Test
    void testGetUser_invalidJson_returnsNull() throws Exception {
        String responseJson = "not valid json {{{";

        try (MockedConstruction<OkHttpClient> mocked = mockConstruction(OkHttpClient.class,
                (mock, context) -> {
                    Call mockCall = mock(Call.class);
                    ResponseBody mockBody = mock(ResponseBody.class);
                    when(mockBody.string()).thenReturn(responseJson);
                    Response response = new Response.Builder()
                            .request(new Request.Builder().url("https://api.github.com/user").build())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(mockBody)
                            .build();
                    when(mock.newCall(any())).thenReturn(mockCall);
                    when(mockCall.execute()).thenReturn(response);
                })) {

            GithubUser user = githubProvider.getUser("test_token");

            assertNull(user);
        }
    }

    @Test
    void testGetUser_emptyResponse_returnsNull() throws Exception {
        try (MockedConstruction<OkHttpClient> mocked = mockConstruction(OkHttpClient.class,
                (mock, context) -> {
                    Call mockCall = mock(Call.class);
                    ResponseBody mockBody = mock(ResponseBody.class);
                    when(mockBody.string()).thenReturn("");
                    Response response = new Response.Builder()
                            .request(new Request.Builder().url("https://api.github.com/user").build())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(mockBody)
                            .build();
                    when(mock.newCall(any())).thenReturn(mockCall);
                    when(mockCall.execute()).thenReturn(response);
                })) {

            GithubUser user = githubProvider.getUser("test_token");

            assertNull(user);
        }
    }
}
