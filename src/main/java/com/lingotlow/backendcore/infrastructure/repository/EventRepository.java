package com.lingotlow.backendcore.infrastructure.repository;

import com.lingotlow.backendcore.domain.event.model.Event;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
}