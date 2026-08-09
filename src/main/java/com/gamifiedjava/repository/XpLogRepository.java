package com.gamifiedjava.repository;

import com.gamifiedjava.model.XpLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface XpLogRepository extends JpaRepository<XpLog, Integer> {
    List<XpLog> findAllByOrderByCreatedAtDesc();
}
