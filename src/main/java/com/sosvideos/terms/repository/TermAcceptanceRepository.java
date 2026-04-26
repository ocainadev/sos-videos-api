package com.sosvideos.terms.repository;

import com.sosvideos.terms.entity.TermAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TermAcceptanceRepository extends JpaRepository<TermAcceptance, Long> {

    boolean existsByUserIdAndTermId(Long userId, Long termId);

    @Query("""
            SELECT ta FROM TermAcceptance ta
            WHERE ta.user.id = :userId
              AND ta.term.active = true
            ORDER BY ta.acceptedAt DESC
            """)
    Optional<TermAcceptance> findActiveTermAcceptanceByUserId(@Param("userId") Long userId);
}
