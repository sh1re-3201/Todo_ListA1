package com.tempest.todo_lista1.todo;

import java.time.Instant;
import java.time.LocalDate;

public record TodoResponse(
        Long id,
        String title,
        String description,
        boolean completed,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt
) {

    public static TodoResponse from(Todo t) {
        return new TodoResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.isCompleted(),
                t.getDueDate(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
