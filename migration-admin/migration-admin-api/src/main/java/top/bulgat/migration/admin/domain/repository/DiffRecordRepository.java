package top.bulgat.migration.admin.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import top.bulgat.migration.admin.domain.model.DiffRecord;
import top.bulgat.migration.admin.domain.model.DiffStatisticsPoint;

/**
 * DiffRecordRepository 定义持久化访问能力。
 */
public interface DiffRecordRepository {

    DiffRecord save(DiffRecord record);

    Optional<DiffRecord> findById(long id);

    List<DiffRecord> findByCondition(
            String migrationKey,
            Integer hasDiff,
            Integer migrationStatus,
            String traceId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int pageSize,
            boolean selectFull);

    long countByCondition(
            String migrationKey,
            Integer hasDiff,
            Integer migrationStatus,
            String traceId,
            LocalDateTime startDate,
            LocalDateTime endDate);

    List<DiffStatisticsPoint> calculateTrendStatistics(
            String migrationKey,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer status,
            int granularitySeconds);
}

