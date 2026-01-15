package com.softropic.promora.email.persistence.entity;

/**
 * Email delivery status enumeration.
 * Represents the status of email delivery attempts.
 */
public enum EmailDeliveryStatus {
    SENT,
    FAILED,
    SENDING,
    DELETE
}
