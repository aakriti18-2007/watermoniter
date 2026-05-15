package com.example.watermoniter.repository;

import com.example.watermoniter.model.Project;
import com.example.watermoniter.model.ProjectMember;
import com.example.watermoniter.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectAndUser(Project project, AppUser user);

    List<ProjectMember> findByProjectOrderByUserDisplayName(Project project);

    void deleteByProjectAndUser(Project project, AppUser user);
}
