package com.ecom.notifications.services;

import com.ecom.notifications.clients.UserClient;
import com.ecom.notifications.dtos.EventDto;
import com.ecom.notifications.exceptions.InvalidEventException;
import com.ecom.notifications.models.EventType;
import com.ecom.notifications.models.Notification;
import com.ecom.notifications.models.NotificationStatus;
import com.ecom.notifications.repos.NotificationRepo;
import com.ecom.notifications.senders.EmailSender;
import com.ecom.notifications.templates.NotificationTemplates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    private NotificationRepo repo;
    private EmailSender sender;
    private UserClient userClient;
    private NotificationService service;
    private final List<NotificationStatus> savedStatuses = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repo = mock(NotificationRepo.class);
        sender = mock(EmailSender.class);
        userClient = mock(UserClient.class);
        service = new NotificationService(repo, new NotificationTemplates(), sender, userClient, 5);
        when(repo.save(any())).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            savedStatuses.add(n.getStatus());
            return n;
        });
    }

    private static EventDto event(String id, String type, Map<String, Object> data) {
        EventDto e = new EventDto();
        e.setEventId(id);
        e.setType(type);
        e.setData(data);
        return e;
    }

    private static Map<String, Object> data(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    void rejectsInvalidEnvelopes() {
        assertThatThrownBy(() -> service.handleEvent(event(null, "USER_REGISTERED", data("userId", 1))))
                .isInstanceOf(InvalidEventException.class).hasMessage("eventId is required");
        assertThatThrownBy(() -> service.handleEvent(event("e", "NOPE", data("userId", 1))))
                .hasMessage("Unknown event type: NOPE");
        assertThatThrownBy(() -> service.handleEvent(event("e", "ORDER_SHIPPED", data("orderId", 1))))
                .hasMessage("data.userId is required");
        verifyNoInteractions(sender);
    }

    @Test
    void duplicateEventIsIgnored() throws Exception {
        when(repo.existsByEventId("dup")).thenReturn(true);
        assertThat(service.handleEvent(event("dup", "USER_REGISTERED", data("userId", 1, "email", "a@x.com")))).isFalse();
        verify(repo, never()).save(any());
        verifyNoInteractions(sender);
    }

    @Test
    void concurrentDuplicateOnSaveIsIgnored() throws Exception {
        doThrow(new DataIntegrityViolationException("unique")).when(repo).save(any());
        assertThat(service.handleEvent(event("e1", "USER_REGISTERED", data("userId", 1, "email", "a@x.com")))).isFalse();
        verifyNoInteractions(sender);
    }

    @Test
    void usesEmailFromEventWithoutLookup() throws Exception {
        assertThat(service.handleEvent(event("e1", "USER_REGISTERED", data("userId", 1, "email", "a@x.com", "name", "Alice")))).isTrue();
        verify(sender).send(eq("a@x.com"), eq("Welcome to Ecom"), contains("Hi Alice"));
        verifyNoInteractions(userClient);
        assertThat(savedStatuses).containsExactly(NotificationStatus.PENDING, NotificationStatus.SENT);
    }

    @Test
    void looksUpRecipientWhenEventHasOnlyUserId() throws Exception {
        when(userClient.getUser(2L)).thenReturn(new UserClient.UserContact(2L, "Bob", "bob@x.com"));
        service.handleEvent(event("order-1-delivered", "ORDER_DELIVERED", data("userId", 2, "orderId", 1)));
        verify(sender).send(eq("bob@x.com"), eq("Order #1 delivered"), contains("Hi Bob"));
    }

    @Test
    void senderFailureMarksFailed() throws Exception {
        doThrow(new RuntimeException("smtp down")).when(sender).send(any(), any(), any());
        service.handleEvent(event("e1", "USER_REGISTERED", data("userId", 1, "email", "a@x.com")));
        Notification last = lastSaved();
        assertThat(last.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(last.getAttempts()).isEqualTo(1);
        assertThat(last.getLastError()).isEqualTo("smtp down");
        assertThat(last.getEventType()).isEqualTo(EventType.USER_REGISTERED);
    }

    private Notification lastSaved() {
        var captor = org.mockito.ArgumentCaptor.forClass(Notification.class);
        verify(repo, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private static Notification stored(NotificationStatus status, int attempts, long createdAt) {
        Notification n = new Notification();
        n.setStatus(status);
        n.setAttempts(attempts);
        n.setCreatedAt(createdAt);
        n.setRecipientEmail("a@x.com");
        n.setSubject("s");
        n.setBody("b");
        return n;
    }

    @Test
    void retryResendsFailedAndStuckPendingOnly() {
        long now = System.currentTimeMillis();
        Notification failed = stored(NotificationStatus.FAILED, 2, now - 5_000);
        Notification stuck = stored(NotificationStatus.PENDING, 0, now - 120_000);
        Notification fresh = stored(NotificationStatus.PENDING, 0, now - 1_000);
        when(repo.findByStatusAndAttemptsLessThan(NotificationStatus.FAILED, 5)).thenReturn(List.of(failed));
        when(repo.findByStatusAndAttemptsLessThan(NotificationStatus.PENDING, 5)).thenReturn(List.of(stuck, fresh));

        service.retryUndelivered();

        verify(sender, times(2)).send(any(), any(), any());
        assertThat(failed.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(failed.getAttempts()).isEqualTo(3);
        assertThat(stuck.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(fresh.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(fresh.getAttempts()).isZero();
    }

    @Test
    void retryQueriesOnlyBelowMaxAttempts() {
        when(repo.findByStatusAndAttemptsLessThan(any(), anyInt())).thenReturn(List.of());
        service.retryUndelivered();
        verify(repo).findByStatusAndAttemptsLessThan(NotificationStatus.FAILED, 5);
        verify(repo).findByStatusAndAttemptsLessThan(NotificationStatus.PENDING, 5);
        verifyNoInteractions(sender);
    }
}
