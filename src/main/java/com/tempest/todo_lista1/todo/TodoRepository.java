package com.tempest.todo_lista1.todo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    @Query("""
            SELECT t FROM Todo t
            ORDER BY t.completed ASC,
                     CASE WHEN t.dueDate IS NULL THEN 1 ELSE 0 END,
                     t.dueDate ASC,
                     t.createdAt DESC,
                     t.id DESC
            """)
    List<Todo> findAllOrdered();
}
