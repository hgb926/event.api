package com.study.event.api.event.repository;


import com.study.event.api.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository
        extends JpaRepository<Event, Long>, EventRepositoryCustom {


}
