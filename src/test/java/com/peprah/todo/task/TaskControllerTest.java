package com.peprah.todo.task;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TaskService service;

    // ── Happy paths ───────────────────────────────────────────────────────────

    @Test
    void getAllTasks_returnsOkWithList() throws Exception {
        given(service.getAllTasks()).willReturn(List.of(task(1L, "Buy milk")));

        mvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Buy milk"));
    }

    @Test
    void getTask_returnsOk() throws Exception {
        given(service.getTask(1L)).willReturn(task(1L, "Buy milk"));

        mvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Buy milk"));
    }

    @Test
    void createTask_returnsCreated() throws Exception {
        given(service.createTask(any(Task.class))).willReturn(task(1L, "Buy milk"));

        mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Buy milk\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Buy milk"));
    }

    @Test
    void updateTask_returnsOk() throws Exception {
        Task updated = task(1L, "Buy oat milk");
        updated.setCompleted(true);
        given(service.updateTask(eq(1L), any(Task.class))).willReturn(updated);

        mvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Buy oat milk\",\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buy oat milk"))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void deleteTask_returnsNoContent() throws Exception {
        mvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());

        verify(service).deleteTask(1L);
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    void getTask_notFound_returns404WithErrorShape() throws Exception {
        given(service.getTask(99L))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        mvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void createTask_blankTitle_returns400WithFieldError() throws Exception {
        mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("title")));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static Task task(Long id, String title) {
        Task t = new Task();
        t.setId(id);
        t.setTitle(title);
        return t;
    }
}
