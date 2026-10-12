package com.tempest.todo_lista1.todo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TodoRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description,

        LocalDate dueDate) {

    public TodoRequest {
        title = title == null ? null : title.strip();
        description = (description == null || description.isBlank()) ? null : description.strip();
    }
}
