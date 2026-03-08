package top.bulgat.migration.admin.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import top.bulgat.migration.admin.domain.model.DiffItem;
import top.bulgat.migration.admin.domain.model.DiffRecord;
import top.bulgat.migration.admin.domain.repository.DiffRecordRepository;
import top.bulgat.migration.config.common.model.dataobject.DiffRecordDO;
import top.bulgat.migration.config.common.model.enums.DiffType;
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
     * @param record 记录实体。
     * @return 返回结果。
     */
    @Override
    public DiffRecord save(DiffRecord record) {
        DiffRecordDO dataObject = toDataObject(record);
        diffRecordDAO.insert(dataObject);
        log.info("diff_record.save id={}, migrationKey={}, hasDiff={}",
                dataObject.getId(), dataObject.getMigrationKey(), dataObject.getHasDiff());
        return toDomain(dataObject);
    }

    /**
     * 执行 findById 业务逻辑。
     * 
     * @param id 记录 ID。
     * @return 返回结果。
     */
    @Override
    public Optional<DiffRecord> findById(long id) {
        DiffRecordDO dataObject = diffRecordDAO.selectById(id);
        log.info("diff_record.findById id={}, found={}", id, dataObject != null);
        return dataObject == null ? Optional.empty() : Optional.of(toDomain(dataObject));
    }

    @Override
    public List<DiffRecord> findByCondition(
            String migrationKey,
            Integer hasDiff,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        LambdaQueryWrapper<DiffRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DiffRecordDO::getMigrationKey, migrationKey);
        if (hasDiff != null) {
            wrapper.eq(DiffRecordDO::getHasDiff, hasDiff);
        }
        if (startDate != null) {
            wrapper.ge(DiffRecordDO::getCreateTime, startDate);
        }
        if (endDate != null) {
            wrapper.le(DiffRecordDO::getCreateTime, endDate);
        }
        wrapper.orderByDesc(DiffRecordDO::getCreateTime);
        List<DiffRecordDO> rows = diffRecordDAO.selectList(wrapper);
        log.info("diff_record.findByCondition migrationKey={}, hasDiff={}, startDate={}, endDate={}, rows={}",
                migrationKey, hasDiff, startDate, endDate, rows.size());
        List<DiffRecord> result = new ArrayList<>();
        for (DiffRecordDO row : rows) {
            result.add(toDomain(row));
        }
        return result;
    }

    private DiffRecordDO toDataObject(DiffRecord record) {
        DiffRecordDO dataObject = new DiffRecordDO();
        dataObject.setId(record.getId() <= 0 ? null : record.getId());
        dataObject.setMigrationKey(record.getMigrationKey());
        dataObject.setTraceId(record.getTraceId());
        dataObject.setOldResponse(record.getOldResponse());
        dataObject.setNewResponse(record.getNewResponse());
        dataObject.setDiffResults(writeDiffItems(record.getDiffResults()));
        dataObject.setHasDiff(record.isHasDiff() ? 1 : 0);
        dataObject.setDiffType(record.getDiffType());
        dataObject.setGrayscaleParam(record.getGrayscaleParam());
        dataObject.setOldCostTimeMs(record.getOldCostTimeMs());
        dataObject.setNewCostTimeMs(record.getNewCostTimeMs());
        dataObject.setTotalCostTimeMs(record.getTotalCostTimeMs());
        dataObject.setCreateTime(record.getCreateTime() == null ? LocalDateTime.now() : record.getCreateTime());
        dataObject.setOldSuccess(record.getOldSuccess() != null && record.getOldSuccess() ? 1 : 0);
        dataObject.setNewSuccess(record.getNewSuccess() != null && record.getNewSuccess() ? 1 : 0);
        dataObject.setOldErrorMessage(record.getOldErrorMessage());
        dataObject.setNewErrorMessage(record.getNewErrorMessage());
        dataObject.setOldRequestParams(record.getOldRequestParams());
        dataObject.setNewRequestParams(record.getNewRequestParams());
        dataObject.setMigrationTaskStatus(record.getMigrationTaskStatus());
        dataObject.setGrayscaleRules(record.getGrayscaleRules());
        dataObject.setGrayscaleHit(record.getGrayscaleHit() != null && record.getGrayscaleHit() ? 1 : 0);
        dataObject.setFallbackTriggered(record.getFallbackTriggered() != null && record.getFallbackTriggered() ? 1 : 0);
        return dataObject;
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
                dataObject.getGrayscaleParam(),
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
                dataObject.getMigrationTaskStatus(),
                dataObject.getGrayscaleRules(),
                Integer.valueOf(1).equals(dataObject.getGrayscaleHit()),
                Integer.valueOf(1).equals(dataObject.getFallbackTriggered()));
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

    private record DiffItemPayload(String fieldPath, String oldValue, String newValue, String diffType) {
    }
}
