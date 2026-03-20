package top.bulgat.migration.diff.domain.repository;

import top.bulgat.migration.diff.domain.model.DiffRecord;
import top.bulgat.migration.diff.domain.model.DiffRequest;
import top.bulgat.migration.diff.domain.model.DiffResult;
import java.util.List;

/**
 * DiffRecordRepository 定义持久化访问能力。
 */
public interface DiffRecordRepository {

    DiffRecord save(DiffRequest request, DiffResult result);

    /**
     * 批量持久化数据。
     *
     * @param requests 请求参数列表。
     * @param results  结果对象列表。
     */
    void saveBatch(List<DiffRequest> requests, List<DiffResult> results);
}
