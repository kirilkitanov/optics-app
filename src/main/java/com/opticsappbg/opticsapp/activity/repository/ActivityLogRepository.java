package com.opticsappbg.opticsapp.activity.repository;

import com.opticsappbg.opticsapp.activity.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;


@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long>,
        JpaSpecificationExecutor<ActivityLog> {

    @Modifying(flushAutomatically = true)
    @Query(value = """
        DELETE FROM activity_logs
        WHERE id NOT IN (
            SELECT id
            FROM activity_logs
            ORDER BY created_on DESC, id DESC
            LIMIT 500
        )
        """, nativeQuery = true)
    void deleteOlderThanLatest500();
}