package top.bulgat.migration.diff.infrastructure.repository.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.bulgat.migration.diff.domain.model.DiffItem;
import top.bulgat.migration.diff.domain.model.DiffRecord;
import top.bulgat.migration.diff.domain.model.DiffRequest;
import top.bulgat.migration.diff.domain.model.DiffResult;
import top.bulgat.migration.diff.domain.model.DiffType;
import top.bulgat.migration.diff.domain.repository.DiffRecordRepository;
import top.bulgat.migration.config.common.model.dataobject.DiffRecordDO;
import top.bulgat.migration.config.common.dal.DiffRecordDAO;

/**
 * MybatisDiffRecordRepository 定义持久化访问能力。
 */
@Repository
public class DefaultDiffRecordRepository implements DiffRecordRepository {

    private static final Logger log = LoggerFactory.getLogger(DefaultDiffRecordRepository.class);
    private final DiffRecordDAO diffRecordDAO;
    private final ObjectMapper objectMapper;

    public DefaultDiffRecordRepository(DiffRecordDAO diffRecordDAO, ObjectMapper objectMapper) {
        this.diffRecordDAO = diffRecordDAO;
        this.objectMapper = objectMapper;
    }

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
                : result.getDiffItems().get(0).diffType().name();
        DiffRecordDO dataObject = new DiffRecordDO();
        dataObject.setMigrationKey(request.getMigrationKey());
        dataObject.setTraceId(request.getTraceId());
        dataObject.setOldResponse(request.getOldJson());
        dataObject.setNewResponse(request.getNewJson());
        dataObject.setDiffResults(writeDiffItems(result.getDiffItems()));
        dataObject.setHasDiff(result.hasDiff() ? 1 : 0);
        dataObject.setDiffType(diffType);
        dataObject.setGrayParam(request.getGrayParam());
        dataObject.setOldCostTimeMs(request.getOldCostTimeMs());
        dataObject.setNewCostTimeMs(request.getNewCostTimeMs());
        dataObject.setTotalCostTimeMs((int) result.getCostTimeMs());
        dataObject.setCreateTime(LocalDateTime.now());
        dataObject.setOldSuccess(request.getOldSuccess() != null && request.getOldSuccess() ? 1 : 0);
        dataObject.setNewSuccess(request.getNewSuccess() != null && request.getNewSuccess() ? 1 : 0);
        dataObject.setOldErrorMessage(request.getOldErrorMessage());
        dataObject.setNewErrorMessage(request.getNewErrorMessage());
        dataObject.setOldRequestParams(request.getOldRequestParams());
        dataObject.setNewRequestParams(request.getNewRequestParams());
        dataObject.setMigrationStatus(request.getMigrationTaskStatus());
        dataObject.setGrayRules(request.getGrayRules());
        dataObject.setGrayHit(request.getGrayHit() != null && request.getGrayHit() ? 1 : 0);
        dataObject
                .setFallbackTriggered(request.getFallbackTriggered() != null && request.getFallbackTriggered() ? 1 : 0);
        diffRecordDAO.insert(dataObject);
        log.info("diff_record.save migrationKey={}, traceId={}, hasDiff={}, diffItemCount={}",
                request.getMigrationKey(), request.getTraceId(), dataObject.getHasDiff(), result.getDiffItems().size());
        return toDomain(dataObject);
    }

    /**
     * 批量持久化数据。利用 Spring 事务包装单个插入，减少提交开销。
     * 同样利用了 BaseMapper 的 insert 方法。
     *
     * @param requests 请求参数列表。
     * @param results  结果对象列表。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(List<DiffRequest> requests, List<DiffResult> results) {
        if (requests == null || results == null || requests.size() != results.size()) {
            throw new IllegalArgumentException("requests and results must be non-null and have the same size");
        }
        for (int i = 0; i < requests.size(); i++) {
            save(requests.get(i), results.get(i));
        }
        log.info("diff_record.saveBatch completed batchSize={}", requests.size());
    }

    private String writeDiffItems(List<DiffItem> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception ex) {
            log.error("diff_record.serializeDiffItems failed size={}", items == null ? null : items.size(), ex);
            throw new IllegalStateException("failed to serialize diff items", ex);
        }
    }

    private List<DiffItem> readDiffItems(String payload) {
        if (payload == null || payload.isBlank()) {
            return List.of();
        }
        try {
            List<DiffItemPayload> payloads = objectMapper.readValue(
                    payload,
                    new TypeReference<List<DiffItemPayload>>() {
                    });
            List<DiffItem> result = new ArrayList<>();
            for (DiffItemPayload item : payloads) {
                result.add(new DiffItem(
                        item.fieldPath(),
                        item.oldValue(),
                        item.newValue(),
                        DiffType.valueOf(item.diffType())));
            }
            return result;
        } catch (Exception ex) {
            log.error("diff_record.deserializeDiffItems failed payloadLength={}", payload.length(), ex);
            throw new IllegalStateException("failed to deserialize diff items", ex);
        }
    }

    private DiffRecord toDomain(DiffRecordDO dataObject) {
        return new DiffRecord(
                dataObject.getId() == null ? 0 : dataObject.getId(),
                dataObject.getMigrationKey(),
                dataObject.getTraceId(),
                dataObject.getOldResponse(),
                dataObject.getNewResponse(),
                readDiffItems(dataObject.getDiffResults()),
                Integer.valueOf(1).equals(dataObject.getHasDiff()),
                dataObject.getDiffType(),
                dataObject.getGrayParam(),
                dataObject.getOldCostTimeMs(),
                dataObject.getNewCostTimeMs(),
                dataObject.getTotalCostTimeMs(),
                dataObject.getCreateTime(),
                Integer.valueOf(1).equals(dataObject.getOldSuccess()),
                Integer.valueOf(1).equals(dataObject.getNewSuccess()),
                dataObject.getOldErrorMessage(),
                dataObject.getNewErrorMessage(),
                dataObject.getOldRequestParams(),
                dataObject.getNewRequestParams(),
                dataObject.getMigrationStatus(),
                dataObject.getGrayRules(),
                Integer.valueOf(1).equals(dataObject.getGrayHit()),
                Integer.valueOf(1).equals(dataObject.getFallbackTriggered()));
    }

    private record DiffItemPayload(String fieldPath, String oldValue, String newValue, String diffType) {
    }
}
