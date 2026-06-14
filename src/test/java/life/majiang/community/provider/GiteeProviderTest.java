package life.majiang.community.provider;

import life.majiang.community.dto.AccessTokenDTO;
import life.majiang.community.provider.dto.GiteeUser;
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
public class GiteeProviderTest {

    private GiteeProvider giteeProvider;
    private AccessTokenDTO accessTokenDTO;

    @BeforeEach
    void setUp() {
        giteeProvider = new GiteeProvider();
        ReflectionTestUtils.setField(giteeProvider, "clientId", "test_gitee_client_id");
        ReflectionTestUtils.setField(giteeProvider, "clientSecret", "test_gitee_client_secret");
        ReflectionTestUtils.setField(giteeProvider, "redirectUri", "http://localhost:8887/callback/gitee");

        accessTokenDTO = new AccessTokenDTO();
        accessTokenDTO.setCode("test_gitee_code");
        accessTokenDTO.setState("test_gitee_state");
    }

    // ==================== getAccessToken ====================

    @Test
    void testGetAccessToken_success() throws Exception {
        String responseJson = "{\"access_token\":\"gitee_token_123\",\"token_type\":\"bearer\",\"expires_in\":86400}";

        try (MockedConstruction<OkHttpClient> mocked = mockConstruction(OkHttpClient.class,
                (mock, context) -> {
                    Call mockCall = mock(Call.class);
                    ResponseBody mockBody = mock(ResponseBody.class);
                    when(mockBody.string()).thenReturn(responseJson);
                    Response response = new Response.Builder()
                            .request(new Request.Builder().url("https://gitee.com/oauth/token").build())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(mockBody)
                            .build();
                    when(mock.newCall(any())).thenReturn(mockCall);
                    when(mockCall.execute()).thenReturn(response);
                })) {

            String token = giteeProvider.getAccessToken(accessTokenDTO);

            assertEquals("gitee_token_123", token);
            assertEquals(1, mocked.constructed().size());
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

            String token = giteeProvider.getAccessToken(accessTokenDTO);

            assertNull(token);
        }
    }

    @Test
    void testGetAccessToken_invalidJson_returnsNull() throws Exception {
        String responseJson = "not_valid_json";

        try (MockedConstruction<OkHttpClient> mocked = mockConstruction(OkHttpClient.class,
                (mock, context) -> {
                    Call mockCall = mock(Call.class);
                    ResponseBody mockBody = mock(ResponseBody.class);
                    when(mockBody.string()).thenReturn(responseJson);
                    Response response = new Response.Builder()
                            .request(new Request.Builder().url("https://gitee.com/oauth/token").build())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(mockBody)
                            .build();
                    when(mock.newCall(any())).thenReturn(mockCall);
                    when(mockCall.execute()).thenReturn(response);
                })) {

            String token = giteeProvider.getAccessToken(accessTokenDTO);

            assertNull(token);
        }
    }

    @Test
    void testGetAccessToken_missingAccessTokenField_returnsNull() throws Exception {
        String responseJson = "{\"token_type\":\"bearer\",\"expires_in\":86400}";

        try (MockedConstruction<OkHttpClient> mocked = mockConstruction(OkHttpClient.class,
                (mock, context) -> {
                    Call mockCall = mock(Call.class);
                    ResponseBody mockBody = mock(ResponseBody.class);
                    when(mockBody.string()).thenReturn(responseJson);
                    Response response = new Response.Builder()
                            .request(new Request.Builder().url("https://gitee.com/oauth/token").build())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(mockBody)
                            .build();
                    when(mock.newCall(any())).thenReturn(mockCall);
                    when(mockCall.execute()).thenReturn(response);
                })) {

            String token = giteeProvider.getAccessToken(accessTokenDTO);

            assertNull(token);
        }
    }

    @Test
    void testGetAccessToken_errorResponse_returnsNull() throws Exception {
        String responseJson = "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid authorization code\"}";

        try (MockedConstruction<OkHttpClient> mocked = mockConstruction(OkHttpClient.class,
                (mock, context) -> {
                    Call mockCall = mock(Call.class);
                    ResponseBody mockBody = mock(ResponseBody.class);
                    when(mockBody.string()).thenReturn(responseJson);
                    Response response = new Response.Builder()
                            .request(new Request.Builder().url("https://gitee.com/oauth/token").build())
                            .protocol(Protocol.HTTP_1_1)
                            .code(400)
                            .message("Bad Request")
                            .body(mockBody)
                            .build();
                    when(mock.newCall(any())).thenReturn(mockCall);
                    when(mockCall.execute()).thenReturn(response);
                })) {

            String token = giteeProvider.getAccessToken(accessTokenDTO);

            assertNull(token);
        }
    }

    // ==================== getUser ====================

    @Test
    void testGetUser_success() throws Exception {
        String responseJson = "{\"id\":67890,\"name\":\"Gitee User\",\"bio\":\"Gitee developer\",\"avatarUrl\":\"http://gitee.com/avatar.png\"}";

        try (MockedConstruction<OkHttpClient> mocked = mockConstruction(OkHttpClient.class,
                (mock, context) -> {
                    Call mockCall = mock(Call.class);
                    ResponseBody mockBody = mock(ResponseBody.class);
                    when(mockBody.string()).thenReturn(responseJson);
                    Response response = new Response.Builder()
                            .request(new Request.Builder().url("https://gitee.com/api/v5/user").build())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(mockBody)
                            .build();
                    when(mock.newCall(any())).thenReturn(mockCall);
                    when(mockCall.execute()).thenReturn(response);
                })) {

            GiteeUser user = giteeProvider.getUser("gitee_test_token");

            assertNotNull(user);
            assertEquals(Long.valueOf(67890L), user.getId());
            assertEquals("Gitee User", user.getName());
            assertEquals("Gitee developer", user.getBio());
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

            GiteeUser user = giteeProvider.getUser("gitee_test_token");

            assertNull(user);
        }
    }

    @Test
    void testGetUser_invalidJson_returnsNull() throws Exception {
        String responseJson = "<<<invalid json>>>";

        try (MockedConstruction<OkHttpClient> mocked = mockConstruction(OkHttpClient.class,
                (mock, context) -> {
                    Call mockCall = mock(Call.class);
                    ResponseBody mockBody = mock(ResponseBody.class);
                    when(mockBody.string()).thenReturn(responseJson);
                    Response response = new Response.Builder()
                            .request(new Request.Builder().url("https://gitee.com/api/v5/user").build())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(mockBody)
                            .build();
                    when(mock.newCall(any())).thenReturn(mockCall);
                    when(mockCall.execute()).thenReturn(response);
                })) {

            GiteeUser user = giteeProvider.getUser("gitee_test_token");

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
                            .request(new Request.Builder().url("https://gitee.com/api/v5/user").build())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(mockBody)
                            .build();
                    when(mock.newCall(any())).thenReturn(mockCall);
                    when(mockCall.execute()).thenReturn(response);
                })) {

            GiteeUser user = giteeProvider.getUser("gitee_test_token");

            assertNull(user);
        }
    }

    @Test
    void testGetUser_partialJson_parsesAvailableFields() throws Exception {
        String responseJson = "{\"id\":11111,\"name\":\"Partial User\"}";

        try (MockedConstruction<OkHttpClient> mocked = mockConstruction(OkHttpClient.class,
                (mock, context) -> {
                    Call mockCall = mock(Call.class);
                    ResponseBody mockBody = mock(ResponseBody.class);
                    when(mockBody.string()).thenReturn(responseJson);
                    Response response = new Response.Builder()
                            .request(new Request.Builder().url("https://gitee.com/api/v5/user").build())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(mockBody)
                            .build();
                    when(mock.newCall(any())).thenReturn(mockCall);
                    when(mockCall.execute()).thenReturn(response);
                })) {

            GiteeUser user = giteeProvider.getUser("gitee_test_token");

            assertNotNull(user);
            assertEquals(Long.valueOf(11111L), user.getId());
            assertEquals("Partial User", user.getName());
            assertNull(user.getBio());
        }
    }
}
