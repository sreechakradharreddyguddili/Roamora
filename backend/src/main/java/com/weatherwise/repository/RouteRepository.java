package com.weatherwise.repository;

import com.weatherwise.entity.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    Page<Route> findByUserIdOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );

    Page<Route> findByUserIdAndSavedTrueOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );

    Optional<Route> findByIdAndUserId(
            Long id,
            Long userId
    );

    Optional<Route> findByIdAndUserIdAndSavedTrue(
            Long id,
            Long userId
    );

    boolean existsByIdAndUserId(
            Long id,
            Long userId
    );

    void deleteByIdAndUserId(
            Long id,
            Long userId
    );

    void deleteByUserIdAndSavedFalse(
            Long userId
    );
}