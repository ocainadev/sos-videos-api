package com.sosvideos.datenote.repository;

import com.sosvideos.datenote.entity.DateNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DateNoteRepository extends JpaRepository<DateNote, Long> {
    Optional<DateNote> findByDate(LocalDate date);
}
