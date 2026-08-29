package com.exelynt.booking.dto;

import com.exelynt.booking.model.Status;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReservationUpdateRequest {

    private Status status;

    @DecimalMin(value = "0.0", inclusive = false, message = "price must be greater than 0")
    @Digits(integer = 17, fraction = 2, message = "price must have at most 2 decimal places")
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