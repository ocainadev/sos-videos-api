package com.sosvideos.drive.repository;

import com.sosvideos.drive.entity.DriveFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DriveFolderRepository extends JpaRepository<DriveFolder, Long> {

    Optional<DriveFolder> findByPublishDate(LocalDate publishDate);

    boolean existsByPublishDate(LocalDate publishDate);

    @Query("SELECT f FROM DriveFolder f WHERE f.publishDate < :cutoff")
    List<DriveFolder> findExpired(@Param("cutoff") LocalDate cutoff);
}
