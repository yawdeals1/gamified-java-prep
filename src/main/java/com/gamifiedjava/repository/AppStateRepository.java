package com.gamifiedjava.repository;

import com.gamifiedjava.model.AppState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppStateRepository extends JpaRepository<AppState, Integer> {
}
