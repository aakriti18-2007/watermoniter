package com.example.watermoniter.repository;

import com.example.watermoniter.model.Project;
import com.example.watermoniter.model.TaskItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskItemRepository extends JpaRepository<TaskItem, Long> {

    List<TaskItem> findByProjectOrderByDueDateAsc(Project project);

    @Query("select t from TaskItem t join t.project.members m where m.user.id = :userId order by t.dueDate asc")
    List<TaskItem> findVisibleTasks(Long userId);
}
