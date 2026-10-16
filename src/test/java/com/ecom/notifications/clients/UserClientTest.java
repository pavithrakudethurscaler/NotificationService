package com.ecom.notifications.clients;

import com.ecom.notifications.exceptions.InvalidEventException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class UserClientTest {
    private MockRestServiceServer server;
    private UserClient client;

    @BeforeEach
    void setUp() {
        RestTemplate rt = new RestTemplate();
        server = MockRestServiceServer.bindTo(rt).build();
        client = new UserClient(rt, "http://users", "secret");
    }

    @Test
    void returnsContactWithInternalToken() throws Exception {
        server.expect(requestTo("http://users/internal/api/v1/users/1")).andExpect(header("X-Internal-Token", "secret"))
                .andRespond(withSuccess("{\"id\":1,\"name\":\"Alice\",\"email\":\"a@x.com\"}", MediaType.APPLICATION_JSON));
        assertThat(client.getUser(1L).email()).isEqualTo("a@x.com");
    }

    @Test
    void notFoundIsInvalidEvent() {
        server.expect(requestTo("http://users/internal/api/v1/users/9")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> client.getUser(9L)).isInstanceOf(InvalidEventException.class).hasMessage("User 9 not found");
    }

    @Test
    void serverErrorIsRetryableUserServiceException() {
        server.expect(requestTo("http://users/internal/api/v1/users/1")).andRespond(withServerError());
        assertThatThrownBy(() -> client.getUser(1L)).isInstanceOf(UserServiceException.class);
    }
}
