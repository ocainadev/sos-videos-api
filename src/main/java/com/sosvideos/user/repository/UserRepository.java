package com.sosvideos.user.repository;

import com.sosvideos.user.entity.User;
import com.sosvideos.user.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    long countByRole(UserRole role);

    @Query("""
            SELECT u FROM User u
            WHERE u.role = :role
              AND (:includeInactive = true OR u.active = true)
              AND (CAST(:search AS string) IS NULL
                OR LOWER(u.name)  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<User> findSubscribers(@Param("role") UserRole role,
                               @Param("search") String search,
                               @Param("includeInactive") boolean includeInactive,
                               Pageable pageable);

    @Query(value = """
            SELECT u FROM User u
            LEFT JOIN Subscription s ON s.user.id = u.id
              AND s.createdAt = (SELECT MAX(s2.createdAt) FROM Subscription s2 WHERE s2.user.id = u.id)
            WHERE u.role = :role
              AND (:includeInactive = true OR u.active = true)
              AND (CAST(:search AS string) IS NULL
                OR LOWER(u.name)  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            ORDER BY s.endDate ASC NULLS LAST
            """,
            countQuery = """
            SELECT COUNT(u) FROM User u
            WHERE u.role = :role
              AND (:includeInactive = true OR u.active = true)
              AND (CAST(:search AS string) IS NULL
                OR LOWER(u.name)  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<User> findSubscribersSortedByEndDateAsc(@Param("role") UserRole role,
                                                 @Param("search") String search,
                                                 @Param("includeInactive") boolean includeInactive,
                                                 Pageable pageable);

    @Query(value = """
            SELECT u FROM User u
            LEFT JOIN Subscription s ON s.user.id = u.id
              AND s.createdAt = (SELECT MAX(s2.createdAt) FROM Subscription s2 WHERE s2.user.id = u.id)
            WHERE u.role = :role
              AND (:includeInactive = true OR u.active = true)
              AND (CAST(:search AS string) IS NULL
                OR LOWER(u.name)  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            ORDER BY s.endDate DESC NULLS LAST
            """,
            countQuery = """
            SELECT COUNT(u) FROM User u
            WHERE u.role = :role
              AND (:includeInactive = true OR u.active = true)
              AND (CAST(:search AS string) IS NULL
                OR LOWER(u.name)  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<User> findSubscribersSortedByEndDateDesc(@Param("role") UserRole role,
                                                  @Param("search") String search,
                                                  @Param("includeInactive") boolean includeInactive,
                                                  Pageable pageable);
}
