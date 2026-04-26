package com.sosvideos.download.repository;

import com.sosvideos.download.entity.Download;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface DownloadRepository extends JpaRepository<Download, Long> {

    boolean existsByUserIdAndVideoId(Long userId, Long videoId);

    @Query("SELECT d.video.id FROM Download d WHERE d.user.id = :userId AND d.video.id IN :videoIds")
    Set<Long> findDownloadedVideoIds(@Param("userId") Long userId,
                                     @Param("videoIds") List<Long> videoIds);

    List<Download> findByUserId(Long userId);

    void deleteByVideoId(Long videoId);
}
