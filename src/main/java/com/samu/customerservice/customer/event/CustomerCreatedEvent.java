package com.samu.customerservice.customer.event;

public class CustomerCreatedEvent {

    private String eventId;
    private String eventType;
    private Long customerId;
    private String createdAt;

    public CustomerCreatedEvent() {
    }

    public CustomerCreatedEvent(String eventId, String eventType, Long customerId, String createdAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.customerId = customerId;
        this.createdAt = createdAt;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
