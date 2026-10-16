package com.ecom.notifications.models;

/** Events other services publish. Each maps to one customer notification. */
public enum EventType {
    /** User Service, on signup (HLD 3.1: welcome email) */
    USER_REGISTERED,
    /** User Service, on forgot-password (PRD 1.4) */
    PASSWORD_RESET_REQUESTED,
    /** Order Service, when the order becomes PAID (PRD 4.1) */
    ORDER_CONFIRMED,
    /** Order Service, fulfilment updates (PRD 4.3) */
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    /** Payment Service, when a payment is received (PRD 5.3 receipt) */
    PAYMENT_RECEIVED,
    /** Payment Service, when a payment could not be applied to its order and will be refunded */
    PAYMENT_REFUND_PENDING
}
