package com.sosvideos.terms.repository;

import com.sosvideos.terms.entity.TermAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface TermAcceptanceRepository extends JpaRepository<TermAcceptance, Long> {

    boolean existsByUserIdAndTermId(Long userId, Long termId);

    void deleteByTermId(Long termId);

    @Query("""
            SELECT ta FROM TermAcceptance ta
            WHERE ta.user.id = :userId
              AND ta.term.active = true
            ORDER BY ta.acceptedAt DESC
            """)
    Optional<TermAcceptance> findActiveTermAcceptanceByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT ta.user.id FROM TermAcceptance ta
            WHERE ta.user.id IN :userIds
              AND ta.term.active = true
            """)
    Set<Long> findUserIdsWithActiveTermAcceptance(@Param("userIds") Set<Long> userIds);
}
