package com.peprah.todo.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "tasks", key = "'all'")
    public List<Task> getAllTasks() {
        log.info("CACHE MISS — hitting database for task list");
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Cacheable(value = "task", key = "#id")
    public Task getTask(Long id) {
        log.info("CACHE MISS — hitting database for task {}", id);
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    @Caching(evict = {
        @CacheEvict(value = "tasks", key = "'all'"),
        @CacheEvict(value = "task", key = "#result.id")
    })
    public Task createTask(Task task) {
        return repository.save(task);
    }

    @Caching(evict = {
        @CacheEvict(value = "tasks", key = "'all'"),
        @CacheEvict(value = "task", key = "#id")
    })
    public Task updateTask(Long id, Task updated) {
        Task task = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        task.setTitle(updated.getTitle());
        task.setDescription(updated.getDescription());
        task.setCompleted(updated.isCompleted());
        return repository.save(task);
    }

    @Caching(evict = {
        @CacheEvict(value = "tasks", key = "'all'"),
        @CacheEvict(value = "task", key = "#id")
    })
    public void deleteTask(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        repository.deleteById(id);
    }
}
