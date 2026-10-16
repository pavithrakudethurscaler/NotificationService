package com.ecom.notifications.clients;

import com.ecom.notifications.exceptions.InvalidEventException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Looks up a user's email and name. Order and Payment Service events only carry a user id,
 * because those services never store customers' contact details.
 */
@Component
public class UserClient {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserContact(Long id, String name, String email) {
    }

    private final RestTemplate restTemplate;
    private final String userServiceBaseUrl;
    private final String internalApiToken;

    public UserClient(RestTemplate restTemplate,
                      @Value("${userservice.base-url}") String userServiceBaseUrl,
                      @Value("${internal.api-token:}") String internalApiToken) {
        this.restTemplate = restTemplate;
        this.userServiceBaseUrl = userServiceBaseUrl;
        this.internalApiToken = internalApiToken;
    }

    public UserContact getUser(Long userId) throws InvalidEventException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", internalApiToken);
        try {
            UserContact user = restTemplate.exchange(userServiceBaseUrl + "/internal/api/v1/users/{id}", HttpMethod.GET,
                    new HttpEntity<>(headers), UserContact.class, userId).getBody();
            if (user == null || user.email() == null) {
                throw new InvalidEventException("User " + userId + " has no email address");
            }
            return user;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new InvalidEventException("User " + userId + " not found");
            }
            throw new UserServiceException("User Service rejected the lookup for user " + userId, e);
        } catch (RestClientException e) {
            throw new UserServiceException("User Service is unavailable", e);
        }
    }
}
