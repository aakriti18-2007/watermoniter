package com.example.watermoniter.repository;

import com.example.watermoniter.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("select distinct p from Project p join p.members m where m.user.id = :userId order by p.createdAt desc")
    List<Project> findVisibleProjects(Long userId);
}
