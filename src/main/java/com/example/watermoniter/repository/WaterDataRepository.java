package com.example.watermoniter.repository;

import com.example.watermoniter.model.WaterData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WaterDataRepository extends JpaRepository<WaterData, Long> {

    List<WaterData> findTop20ByOrderByRecordedAtAsc();

    WaterData findTopByOrderByRecordedAtDesc();
}
