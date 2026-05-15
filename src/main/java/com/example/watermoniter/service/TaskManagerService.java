package com.example.watermoniter.service;

import com.example.watermoniter.model.AppUser;
import com.example.watermoniter.model.Project;
import com.example.watermoniter.model.ProjectMember;
import com.example.watermoniter.model.TaskItem;
import com.example.watermoniter.repository.ProjectMemberRepository;
import com.example.watermoniter.repository.ProjectRepository;
import com.example.watermoniter.repository.TaskItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TaskManagerService {

    private static final List<String> STATUSES = List.of("TO_DO", "IN_PROGRESS", "DONE");
    private static final List<String> PRIORITIES = List.of("LOW", "MEDIUM", "HIGH");

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskItemRepository taskItemRepository;
    private final AppUserService appUserService;

    public TaskManagerService(ProjectRepository projectRepository,
                              ProjectMemberRepository projectMemberRepository,
                              TaskItemRepository taskItemRepository,
                              AppUserService appUserService) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.taskItemRepository = taskItemRepository;
        this.appUserService = appUserService;
    }

    @Transactional
    public Map<String, Object> createProject(String username, Map<String, String> request) {
        AppUser user = appUserService.findDomainUser(username);
        Project project = new Project();
        project.setName(required(request.get("name"), "Project name is required"));
        project.setDescription(clean(request.get("description")));
        project.setCreatedBy(user);

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole("ADMIN");
        project.getMembers().add(member);

        return projectResponse(projectRepository.save(project), user);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listProjects(String username) {
        AppUser user = appUserService.findDomainUser(username);
        return projectRepository.findVisibleProjects(user.getId()).stream()
                .map(project -> projectResponse(project, user))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProject(String username, Long projectId) {
        AppUser user = appUserService.findDomainUser(username);
        Project project = findProject(projectId);
        requireMember(project, user);
        return projectResponse(project, user);
    }

    @Transactional
    public Map<String, Object> addMember(String username, Long projectId, Map<String, String> request) {
        AppUser currentUser = appUserService.findDomainUser(username);
        Project project = findProject(projectId);
        requireProjectAdmin(project, currentUser);

        AppUser memberUser = appUserService.findDomainUser(required(request.get("email"), "Member email is required").toLowerCase());
        if (projectMemberRepository.findByProjectAndUser(project, memberUser).isPresent()) {
            throw new IllegalArgumentException("User is already a project member");
        }

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(memberUser);
        member.setRole("MEMBER");
        return memberResponse(projectMemberRepository.save(member));
    }

    @Transactional
    public void removeMember(String username, Long projectId, Long userId) {
        AppUser currentUser = appUserService.findDomainUser(username);
        Project project = findProject(projectId);
        requireProjectAdmin(project, currentUser);
        AppUser user = appUserService.findById(userId);

        ProjectMember membership = projectMemberRepository.findByProjectAndUser(project, user)
                .orElseThrow(() -> new IllegalArgumentException("User is not a project member"));
        if ("ADMIN".equals(membership.getRole())) {
            throw new IllegalArgumentException("Project admin cannot be removed");
        }
        projectMemberRepository.delete(membership);
    }

    @Transactional
    public Map<String, Object> createTask(String username, Long projectId, Map<String, String> request) {
        AppUser currentUser = appUserService.findDomainUser(username);
        Project project = findProject(projectId);
        requireProjectAdmin(project, currentUser);

        TaskItem task = new TaskItem();
        applyAdminTaskFields(task, project, request);
        task.setProject(project);
        task.setCreatedBy(currentUser);
        return taskResponse(taskItemRepository.save(task), currentUser);
    }

    @Transactional
    public Map<String, Object> updateTask(String username, Long projectId, Long taskId, Map<String, String> request) {
        AppUser currentUser = appUserService.findDomainUser(username);
        Project project = findProject(projectId);
        ProjectMember membership = requireMember(project, currentUser);
        TaskItem task = findTask(project, taskId);

        if ("ADMIN".equals(membership.getRole())) {
            applyAdminTaskFields(task, project, request);
        } else {
            if (task.getAssignee() == null || !task.getAssignee().getId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("Members can only update assigned tasks");
            }
            task.setStatus(validateStatus(request.get("status")));
        }

        return taskResponse(taskItemRepository.save(task), currentUser);
    }

    @Transactional
    public void deleteTask(String username, Long projectId, Long taskId) {
        AppUser currentUser = appUserService.findDomainUser(username);
        Project project = findProject(projectId);
        requireProjectAdmin(project, currentUser);
        taskItemRepository.delete(findTask(project, taskId));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard(String username) {
        AppUser currentUser = appUserService.findDomainUser(username);
        List<TaskItem> visibleTasks = taskItemRepository.findVisibleTasks(currentUser.getId());
        LocalDate today = LocalDate.now();

        Map<String, Long> byStatus = STATUSES.stream()
                .collect(Collectors.toMap(Function.identity(), status -> 0L, (a, b) -> a, LinkedHashMap::new));
        visibleTasks.stream()
                .collect(Collectors.groupingBy(TaskItem::getStatus, Collectors.counting()))
                .forEach(byStatus::put);

        Map<String, Long> perUser = visibleTasks.stream()
                .collect(Collectors.groupingBy(task -> task.getAssignee() == null ? "Unassigned" : task.getAssignee().getDisplayName(),
                        LinkedHashMap::new,
                        Collectors.counting()));

        long overdue = visibleTasks.stream()
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(today))
                .filter(task -> !"DONE".equals(task.getStatus()))
                .count();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalTasks", visibleTasks.size());
        response.put("tasksByStatus", byStatus);
        response.put("tasksPerUser", perUser);
        response.put("overdueTasks", overdue);
        response.put("myAssignedTasks", visibleTasks.stream()
                .filter(task -> task.getAssignee() != null && task.getAssignee().getId().equals(currentUser.getId()))
                .map(task -> taskResponse(task, currentUser))
                .toList());
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listUsers() {
        return appUserService.listUsers().stream()
                .map(this::userResponse)
                .toList();
    }

    private void applyAdminTaskFields(TaskItem task, Project project, Map<String, String> request) {
        task.setTitle(required(request.get("title"), "Task title is required"));
        task.setDescription(clean(request.get("description")));
        task.setDueDate(parseDueDate(request.get("dueDate")));
        task.setPriority(validatePriority(request.getOrDefault("priority", "MEDIUM")));
        task.setStatus(validateStatus(request.getOrDefault("status", task.getStatus())));

        String assigneeId = request.get("assigneeId");
        if (assigneeId == null || assigneeId.isBlank()) {
            task.setAssignee(null);
            return;
        }

        AppUser assignee = appUserService.findById(Long.parseLong(assigneeId));
        requireMember(project, assignee);
        task.setAssignee(assignee);
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
    }

    private TaskItem findTask(Project project, Long taskId) {
        TaskItem task = taskItemRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        if (!task.getProject().getId().equals(project.getId())) {
            throw new IllegalArgumentException("Task does not belong to this project");
        }
        return task;
    }

    private ProjectMember requireProjectAdmin(Project project, AppUser user) {
        ProjectMember membership = requireMember(project, user);
        if (!"ADMIN".equals(membership.getRole())) {
            throw new IllegalArgumentException("Only project admins can perform this action");
        }
        return membership;
    }

    private ProjectMember requireMember(Project project, AppUser user) {
        return projectMemberRepository.findByProjectAndUser(project, user)
                .orElseThrow(() -> new IllegalArgumentException("Project access denied"));
    }

    private Map<String, Object> projectResponse(Project project, AppUser viewer) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", project.getId());
        response.put("name", project.getName());
        response.put("description", project.getDescription());
        response.put("createdAt", project.getCreatedAt());
        response.put("role", requireMember(project, viewer).getRole());
        response.put("members", projectMemberRepository.findByProjectOrderByUserDisplayName(project).stream()
                .map(this::memberResponse)
                .toList());
        response.put("tasks", taskItemRepository.findByProjectOrderByDueDateAsc(project).stream()
                .map(task -> taskResponse(task, viewer))
                .toList());
        return response;
    }

    private Map<String, Object> taskResponse(TaskItem task, AppUser viewer) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", task.getId());
        response.put("title", task.getTitle());
        response.put("description", task.getDescription());
        response.put("dueDate", task.getDueDate());
        response.put("priority", task.getPriority());
        response.put("status", task.getStatus());
        response.put("projectId", task.getProject().getId());
        response.put("projectName", task.getProject().getName());
        response.put("assignee", task.getAssignee() == null ? null : userResponse(task.getAssignee()));
        response.put("createdBy", userResponse(task.getCreatedBy()));
        response.put("canEdit", "ADMIN".equals(requireMember(task.getProject(), viewer).getRole()));
        response.put("canUpdateStatus", task.getAssignee() != null && task.getAssignee().getId().equals(viewer.getId()));
        return response;
    }

    private Map<String, Object> memberResponse(ProjectMember member) {
        Map<String, Object> response = userResponse(member.getUser());
        response.put("projectRole", member.getRole());
        return response;
    }

    private Map<String, Object> userResponse(AppUser user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getUsername());
        response.put("username", user.getUsername());
        response.put("displayName", user.getDisplayName());
        response.put("role", user.getRole());
        return response;
    }

    private String validateStatus(String status) {
        String normalized = required(status, "Task status is required").trim().toUpperCase().replace(" ", "_");
        if (!STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Status must be To Do, In Progress, or Done");
        }
        return normalized;
    }

    private String validatePriority(String priority) {
        String normalized = required(priority, "Priority is required").trim().toUpperCase();
        if (!PRIORITIES.contains(normalized)) {
            throw new IllegalArgumentException("Priority must be Low, Medium, or High");
        }
        return normalized;
    }

    private LocalDate parseDueDate(String dueDate) {
        if (dueDate == null || dueDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(dueDate);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
