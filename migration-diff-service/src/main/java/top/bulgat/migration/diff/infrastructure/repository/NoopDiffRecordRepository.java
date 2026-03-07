package top.bulgat.migration.diff.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;
import top.bulgat.migration.diff.domain.model.DiffRecord;
import top.bulgat.migration.diff.domain.model.DiffRequest;
import top.bulgat.migration.diff.domain.model.DiffResult;
import top.bulgat.migration.diff.domain.repository.DiffRecordRepository;

/**
 * NoopDiffRecordRepository 定义持久化访问能力。
 */
@ConditionalOnMissingBean(DiffRecordRepository.class)
@Repository
public class NoopDiffRecordRepository implements DiffRecordRepository {

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final List<DiffRecord> records = new CopyOnWriteArrayList<>();

    /**
     * 持久化数据。
     * 
     * @param request 请求参数。
     * @param result  结果对象。
     * @return 返回结果。
     */
    @Override
    public DiffRecord save(DiffRequest request, DiffResult result) {
        String diffType = result.getDiffItems().isEmpty()
                ? null
                : result.getDiffItems().get(0).getDiffType().name();
        DiffRecord record = new DiffRecord(
                idGenerator.getAndIncrement(),
                request.getMigrationKey(),
                request.getTraceId(),
                request.getOldJson(),
                request.getNewJson(),
                result.getDiffItems(),
                result.hasDiff(),
                diffType,
                request.getGrayscaleParam(),
                request.getOldCostTimeMs(),
                request.getNewCostTimeMs(),
                (int) result.getCostTimeMs(),
                LocalDateTime.now(),
                request.getOldSuccess(),
                request.getNewSuccess(),
                request.getOldErrorMessage(),
                request.getNewErrorMessage(),
                request.getOldRequestParams(),
                request.getNewRequestParams(),
                request.getMigrationStatus(),
                request.getGrayscaleRules(),
                request.getGrayscaleHit(),
                request.getFallbackTriggered());
        records.add(record);
        return record;
    }
}
