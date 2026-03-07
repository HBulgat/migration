package top.bulgat.migration.admin.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import top.bulgat.migration.admin.domain.model.DiffRecord;

/**
 * DiffRecordRepository 定义持久化访问能力。
 */
public interface DiffRecordRepository {

    DiffRecord save(DiffRecord record);

    Optional<DiffRecord> findById(long id);

    List<DiffRecord> findByCondition(
            String migrationKey,
            Integer hasDiff,
            LocalDateTime startDate,
            LocalDateTime endDate);
}

