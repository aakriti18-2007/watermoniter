package com.example.watermoniter.controller;

import com.example.watermoniter.service.TaskManagerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TeamTaskController {

    private final TaskManagerService taskManagerService;

    public TeamTaskController(TaskManagerService taskManagerService) {
        this.taskManagerService = taskManagerService;
    }

    @GetMapping("/team/users")
    public ResponseEntity<?> users() {
        return ResponseEntity.ok(taskManagerService.listUsers());
    }

    @GetMapping("/projects")
    public ResponseEntity<?> projects(Authentication authentication) {
        return ResponseEntity.ok(taskManagerService.listProjects(authentication.getName()));
    }

    @PostMapping("/projects")
    public ResponseEntity<?> createProject(Authentication authentication, @RequestBody Map<String, String> request) {
        return handleCreated(() -> taskManagerService.createProject(authentication.getName(), request));
    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<?> project(Authentication authentication, @PathVariable Long projectId) {
        return handleOk(() -> taskManagerService.getProject(authentication.getName(), projectId));
    }

    @PostMapping("/projects/{projectId}/members")
    public ResponseEntity<?> addMember(Authentication authentication,
                                       @PathVariable Long projectId,
                                       @RequestBody Map<String, String> request) {
        return handleCreated(() -> taskManagerService.addMember(authentication.getName(), projectId, request));
    }

    @DeleteMapping("/projects/{projectId}/members/{userId}")
    public ResponseEntity<?> removeMember(Authentication authentication,
                                          @PathVariable Long projectId,
                                          @PathVariable Long userId) {
        return handleOk(() -> {
            taskManagerService.removeMember(authentication.getName(), projectId, userId);
            return Map.of("message", "Member removed");
        });
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<?> createTask(Authentication authentication,
                                        @PathVariable Long projectId,
                                        @RequestBody Map<String, String> request) {
        return handleCreated(() -> taskManagerService.createTask(authentication.getName(), projectId, request));
    }

    @PutMapping("/projects/{projectId}/tasks/{taskId}")
    public ResponseEntity<?> updateTask(Authentication authentication,
                                        @PathVariable Long projectId,
                                        @PathVariable Long taskId,
                                        @RequestBody Map<String, String> request) {
        return handleOk(() -> taskManagerService.updateTask(authentication.getName(), projectId, taskId, request));
    }

    @DeleteMapping("/projects/{projectId}/tasks/{taskId}")
    public ResponseEntity<?> deleteTask(Authentication authentication,
                                        @PathVariable Long projectId,
                                        @PathVariable Long taskId) {
        return handleOk(() -> {
            taskManagerService.deleteTask(authentication.getName(), projectId, taskId);
            return Map.of("message", "Task deleted");
        });
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Authentication authentication) {
        return ResponseEntity.ok(taskManagerService.dashboard(authentication.getName()));
    }

    private ResponseEntity<?> handleCreated(Action action) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(action.run());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    private ResponseEntity<?> handleOk(Action action) {
        try {
            return ResponseEntity.ok(action.run());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    private interface Action {
        Object run();
    }
}
