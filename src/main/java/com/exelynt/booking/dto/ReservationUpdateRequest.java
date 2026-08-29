package com.exelynt.booking.dto;

import com.exelynt.booking.model.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * All fields are optional: only the fields the caller actually supplies are
 * applied to the existing reservation. This keeps updates from silently
 * wiping out fields the caller didn't intend to touch.
 */
public class ReservationUpdateRequest {

    private Status status;
    private BigDecimal price;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
