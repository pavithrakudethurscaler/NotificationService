package com.ecom.notifications.templates;

import com.ecom.notifications.exceptions.InvalidEventException;
import com.ecom.notifications.models.EventType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/** Plain-text email content for each event type. */
@Component
public class NotificationTemplates {
    public record Message(String subject, String body) {
    }

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").withZone(ZoneId.of("Asia/Kolkata"));

    public Message render(EventType type, String recipientName, Map<String, Object> data) throws InvalidEventException {
        String hi = "Hi " + (recipientName == null || recipientName.isBlank() ? "there" : recipientName) + ",\n\n";
        String sign = "\n\nThanks,\nThe Ecom team";
        return switch (type) {
            case USER_REGISTERED -> new Message("Welcome to Ecom",
                    hi + "Your account has been created. Happy shopping!" + sign);
            case PASSWORD_RESET_REQUESTED -> new Message("Reset your Ecom password",
                    hi + "Use this link to choose a new password:\n" + require(data, "resetLink")
                            + "\n\nThe link expires at " + time(data, "expiresAt") + ". "
                            + "If you did not ask for this, ignore this email; your password will not change." + sign);
            case ORDER_CONFIRMED -> new Message("Order #" + require(data, "orderId") + " confirmed",
                    hi + "Thank you for your order. We have received your payment of "
                            + money(data, "totalAmount") + " for order #" + data.get("orderId") + ".\n"
                            + "We will let you know when it ships." + sign);
            case ORDER_SHIPPED -> new Message("Order #" + require(data, "orderId") + " has shipped",
                    hi + "Your order #" + data.get("orderId") + " is on its way."
                            + note(data) + sign);
            case ORDER_DELIVERED -> new Message("Order #" + require(data, "orderId") + " delivered",
                    hi + "Your order #" + data.get("orderId") + " has been delivered. Enjoy!" + sign);
            case ORDER_CANCELLED -> new Message("Order #" + require(data, "orderId") + " cancelled",
                    hi + "Your order #" + data.get("orderId") + " has been cancelled." + note(data) + sign);
            case PAYMENT_RECEIVED -> new Message("Payment receipt for order #" + require(data, "orderId"),
                    hi + "Payment received.\n\n"
                            + "Order:      #" + data.get("orderId") + "\n"
                            + "Amount:     " + money(data, "amount") + "\n"
                            + "Payment id: " + require(data, "paymentId") + "\n"
                            + "Reference:  " + data.getOrDefault("gatewayPaymentId", "-") + "\n"
                            + "Paid at:    " + time(data, "paidAt") + sign);
            case PAYMENT_REFUND_PENDING -> new Message("Refund for your payment on order #" + require(data, "orderId"),
                    hi + "We received a payment of " + money(data, "amount") + " for order #" + data.get("orderId")
                            + " that we could not apply, because the order was already paid or cancelled.\n"
                            + "The full amount will be refunded to your original payment method." + sign);
        };
    }

    private static Object require(Map<String, Object> data, String key) throws InvalidEventException {
        Object value = data.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new InvalidEventException("Event data is missing " + key);
        }
        return value;
    }

    private static String money(Map<String, Object> data, String key) throws InvalidEventException {
        long paise = Long.parseLong(require(data, key).toString());
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.of("en", "IN"));
        return format.format(BigDecimal.valueOf(paise).movePointLeft(2));
    }

    private static String time(Map<String, Object> data, String key) throws InvalidEventException {
        return TIME_FORMAT.format(Instant.ofEpochMilli(Long.parseLong(require(data, key).toString()))) + " IST";
    }

    private static String note(Map<String, Object> data) {
        Object note = data.get("note");
        return note == null || note.toString().isBlank() ? "" : "\n" + note;
    }
}
