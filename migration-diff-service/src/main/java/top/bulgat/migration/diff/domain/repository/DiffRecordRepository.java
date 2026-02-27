package top.bulgat.migration.diff.domain.repository;

import top.bulgat.migration.diff.domain.model.DiffRecord;
import top.bulgat.migration.diff.domain.model.DiffRequest;
import top.bulgat.migration.diff.domain.model.DiffResult;

/**
 * DiffRecordRepository defines persistence access.
 */
public interface DiffRecordRepository {

    DiffRecord save(DiffRequest request, DiffResult result);
}
