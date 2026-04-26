package com.sosvideos.tutorial.repository;

import com.sosvideos.tutorial.entity.Tutorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TutorialRepository extends JpaRepository<Tutorial, Long> {

    List<Tutorial> findByActiveTrueOrderBySortOrderAscIdAsc();

    Optional<Tutorial> findByIdAndActiveTrue(Long id);
}
