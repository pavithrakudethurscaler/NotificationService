package com.ecom.notifications.templates;

import com.ecom.notifications.exceptions.InvalidEventException;
import com.ecom.notifications.models.EventType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTemplatesTest {
    private final NotificationTemplates templates = new NotificationTemplates();
    private static final Map<String, Object> FULL = Map.of(
            "orderId", 1, "totalAmount", 99998, "amount", 99998, "paymentId", 7, "paidAt", 1792190429670L,
            "resetLink", "http://x/reset?token=abc", "expiresAt", 1792190429670L, "note", "AWB 12345");

    @ParameterizedTest
    @EnumSource(EventType.class)
    void everyTypeRenders(EventType type) throws Exception {
        NotificationTemplates.Message m = templates.render(type, "Alice", FULL);
        assertThat(m.subject()).isNotBlank();
        assertThat(m.body()).startsWith("Hi Alice,").contains("The Ecom team");
    }

    @Test
    void moneyFormattedInRupees() throws Exception {
        assertThat(templates.render(EventType.PAYMENT_RECEIVED, "A", FULL).body()).contains("₹999.98");
        assertThat(templates.render(EventType.ORDER_CONFIRMED, "A", FULL).body()).contains("₹999.98");
    }

    @Test
    void shippedIncludesNoteAndResetIncludesLink() throws Exception {
        assertThat(templates.render(EventType.ORDER_SHIPPED, "A", FULL).body()).contains("AWB 12345");
        assertThat(templates.render(EventType.PASSWORD_RESET_REQUESTED, "A", FULL).body())
                .contains("http://x/reset?token=abc").contains("IST");
    }

    @Test
    void missingRequiredFieldRejected() {
        assertThatThrownBy(() -> templates.render(EventType.ORDER_SHIPPED, "A", Map.of()))
                .isInstanceOf(InvalidEventException.class).hasMessage("Event data is missing orderId");
        assertThatThrownBy(() -> templates.render(EventType.PASSWORD_RESET_REQUESTED, "A", Map.of()))
                .hasMessage("Event data is missing resetLink");
    }

    @Test
    void blankNameFallsBack() throws Exception {
        assertThat(templates.render(EventType.USER_REGISTERED, " ", Map.of()).body()).startsWith("Hi there,");
    }
}
