package com.samu.customerservice.customer.event;

import com.samu.customerservice.customer.CustomerStatus;

public class CustomerStatusChangeEvent {

    private String eventId;
    private String eventType;
    private Long customerId;
    private CustomerStatus status;

    public CustomerStatusChangeEvent() {
    }

    public CustomerStatusChangeEvent(String eventId, String eventType, Long customerId, CustomerStatus status) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.customerId = customerId;
        this.status = status;
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

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }
}
