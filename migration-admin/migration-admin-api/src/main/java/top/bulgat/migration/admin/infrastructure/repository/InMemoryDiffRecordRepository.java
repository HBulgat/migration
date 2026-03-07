package top.bulgat.migration.admin.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import top.bulgat.migration.admin.domain.model.DiffRecord;
import top.bulgat.migration.admin.domain.repository.DiffRecordRepository;

/**
 * InMemoryDiffRecordRepository defines persistence access.
 */
@Repository
public class InMemoryDiffRecordRepository implements DiffRecordRepository {

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final List<DiffRecord> records = new CopyOnWriteArrayList<>();

    /**
     * Persist data.
     * @param record record entity.
     * @return 返回结果。
     */
    @Override
    public DiffRecord save(DiffRecord record) {
        DiffRecord target = record;
        if (record.getId() <= 0) {
            target = new DiffRecord(
                    idGenerator.getAndIncrement(),
                    record.getMigrationKey(),
                    record.getTraceId(),
                    record.getOldResponse(),
                    record.getNewResponse(),
                    record.getDiffResults(),
                    record.isHasDiff(),
                    record.getDiffType(),
                    record.getGrayscaleParam(),
                    record.getOldCostTimeMs(),
                    record.getNewCostTimeMs(),
                    record.getTotalCostTimeMs(),
                    record.getCreateTime(),
                    record.getOldSuccess(),
                    record.getNewSuccess(),
                    record.getOldErrorMessage(),
                    record.getNewErrorMessage(),
                    record.getOldRequestParams(),
                    record.getNewRequestParams(),
                    record.getMigrationStatus(),
                    record.getGrayscaleRules(),
                    record.getGrayscaleHit(),
                    record.getFallbackTriggered());
        }
        records.add(target);
        return target;
    }

    /**
     * 执行 findById 业务逻辑。
     * @param id record id.
     * @return 返回结果。
     */
    @Override
    public Optional<DiffRecord> findById(long id) {
        return records.stream().filter(item -> item.getId() == id).findFirst();
    }

    @Override
    public List<DiffRecord> findByCondition(
            String migrationKey,
            Integer hasDiff,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        return records.stream()
                .filter(record -> record.getMigrationKey().equals(migrationKey))
                .filter(record -> hasDiff == null || (hasDiff == 1) == record.isHasDiff())
                .filter(record -> startDate == null || !record.getCreateTime().isBefore(startDate))
                .filter(record -> endDate == null || !record.getCreateTime().isAfter(endDate))
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
