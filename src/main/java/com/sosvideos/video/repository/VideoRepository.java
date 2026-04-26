package com.sosvideos.video.repository;

import com.sosvideos.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {

    boolean existsByDriveFileId(String driveFileId);

    @Query("SELECT v FROM Video v WHERE v.driveFileId = :driveFileId")
    java.util.Optional<Video> findByDriveFileId(@Param("driveFileId") String driveFileId);

    /**
     * Returns all active videos within [from, until] — drives the subscriber listing.
     * 'from' = max(today - 7 days, subscription.startDate)
     * 'until' = today
     */
    @Query("""
            SELECT v FROM Video v
            WHERE v.active = true
              AND v.publishDate >= :from
              AND v.publishDate <= :until
            ORDER BY v.publishDate DESC, v.id DESC
            """)
    List<Video> findVisible(@Param("from") LocalDate from,
                            @Param("until") LocalDate until);

    /**
     * Returns active videos for a specific date, respecting subscription start.
     * Used for batch download and single-date queries.
     */
    @Query("""
            SELECT v FROM Video v
            WHERE v.active = true
              AND v.publishDate = :date
              AND v.publishDate >= :subscriptionStart
            ORDER BY v.id ASC
            """)
    List<Video> findVisibleByDate(@Param("date") LocalDate date,
                                  @Param("subscriptionStart") LocalDate subscriptionStart);

    List<Video> findByPublishDateOrderByIdAsc(LocalDate publishDate);
}
