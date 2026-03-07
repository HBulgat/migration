package top.bulgat.migration.diff.domain.repository;

import top.bulgat.migration.diff.domain.model.DiffRecord;
import top.bulgat.migration.diff.domain.model.DiffRequest;
import top.bulgat.migration.diff.domain.model.DiffResult;

/**
 * DiffRecordRepository 定义持久化访问能力。
 */
public interface DiffRecordRepository {

    DiffRecord save(DiffRequest request, DiffResult result);
}
