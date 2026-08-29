package com.exelynt.booking.dto;

import jakarta.validation.constraints.NotBlank;

public class ResourceRequest {

    @NotBlank
    private String title;
    
    private String description;
    private Boolean available;

    public ResourceRequest() {}

    public ResourceRequest(String title, String description, Boolean available) {
        this.title = title;
        this.description = description;
        this.available = available;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
}