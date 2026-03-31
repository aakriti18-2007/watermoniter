package com.example.watermoniter.repository;

import com.example.watermoniter.model.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    List<AiChatMessage> findTop12ByOrderByCreatedAtAsc();
}
