package com.ecom.notifications.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalApiInterceptorTest {
    private static MockHttpServletRequest request(String token) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        if (token != null) {
            r.addHeader(InternalApiInterceptor.INTERNAL_TOKEN_HEADER, token);
        }
        return r;
    }

    @Test
    void acceptsCorrectToken() {
        assertThat(new InternalApiInterceptor("secret").preHandle(request("secret"), new MockHttpServletResponse(), null)).isTrue();
    }

    @Test
    void rejectsMissingOrWrongToken() {
        InternalApiInterceptor i = new InternalApiInterceptor("secret");
        assertThatThrownBy(() -> i.preHandle(request(null), new MockHttpServletResponse(), null)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> i.preHandle(request("nope"), new MockHttpServletResponse(), null)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void failsClosedWhenNotConfigured() {
        assertThatThrownBy(() -> new InternalApiInterceptor("").preHandle(request(""), new MockHttpServletResponse(), null))
                .hasMessage("Internal API token is not configured");
    }
}
