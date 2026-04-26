package com.sosvideos.terms.repository;

import com.sosvideos.terms.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {

    Optional<Term> findFirstByActiveTrue();

    List<Term> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE Term t SET t.active = false WHERE t.active = true")
    void deactivateAll();
}
