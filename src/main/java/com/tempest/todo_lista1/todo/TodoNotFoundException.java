package com.tempest.todo_lista1.todo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class TodoNotFoundException extends ErrorResponseException {

    public TodoNotFoundException(long id) {
        super(HttpStatus.NOT_FOUND,
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Todo " + id + " not found"),
                null);
    }
}
