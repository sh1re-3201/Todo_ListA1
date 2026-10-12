package com.tempest.todo_lista1.todo;

import jakarta.validation.constraints.NotNull;

public record TodoCompletionRequest(
        @NotNull(message = "completed is required")
        Boolean completed
) {}
