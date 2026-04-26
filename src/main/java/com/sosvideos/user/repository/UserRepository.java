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
            ORDER BY u.createdAt DESC
            """)
    Page<User> findSubscribers(@Param("role") UserRole role,
                               @Param("search") String search,
                               @Param("includeInactive") boolean includeInactive,
                               Pageable pageable);
}
