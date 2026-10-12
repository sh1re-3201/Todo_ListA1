package com.tempest.todo_lista1.todo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository repo;

    public TodoService(TodoRepository repo) {
        this.repo = repo;
    }

    public List<TodoResponse> findAll() {
        return repo.findAllOrdered()
                .stream()
                .map(TodoResponse::from)
                .toList();
    }

    public TodoResponse findById(long id) {
        return TodoResponse.from(findOrThrow(id));
    }

    @Transactional
    public TodoResponse create(TodoRequest r) {
        Todo todo = new Todo();
        todo.setTitle(r.title());
        todo.setDescription(r.description());
        todo.setDueDate(r.dueDate());
        todo.setCompleted(false);

        Todo saved = repo.save(todo);
        return TodoResponse.from(saved);
    }

    @Transactional
    public TodoResponse update(long id, TodoRequest r) {
        Todo todo = findOrThrow(id);
        todo.setTitle(r.title());
        todo.setDescription(r.description());
        todo.setDueDate(r.dueDate());
        todo.touch();

        return TodoResponse.from(todo);
    }

    @Transactional
    public TodoResponse setCompleted(long id, boolean value) {
        Todo todo = findOrThrow(id);
        todo.setCompleted(value);
        todo.touch();

        return TodoResponse.from(todo);
    }

    @Transactional
    public void delete(long id) {
        Todo todo = findOrThrow(id);
        repo.delete(todo);
    }

    private Todo findOrThrow(long id) {
        return repo.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }
}
