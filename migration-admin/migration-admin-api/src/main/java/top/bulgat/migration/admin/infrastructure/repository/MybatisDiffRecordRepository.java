package top.bulgat.migration.admin.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import top.bulgat.migration.admin.domain.model.DiffItem;
import top.bulgat.migration.admin.domain.model.DiffRecord;
import top.bulgat.migration.admin.domain.model.DiffType;
import top.bulgat.migration.admin.domain.repository.DiffRecordRepository;
import top.bulgat.migration.admin.infrastructure.persistence.entity.DiffRecordDO;
import top.bulgat.migration.admin.infrastructure.persistence.mapper.DiffRecordMapper;

/**
 * MybatisDiffRecordRepository defines persistence access.
 */
@Primary
@Repository
public class MybatisDiffRecordRepository implements DiffRecordRepository {

    private final DiffRecordMapper diffRecordMapper;
    private final ObjectMapper objectMapper;

    public MybatisDiffRecordRepository(DiffRecordMapper diffRecordMapper, ObjectMapper objectMapper) {
        this.diffRecordMapper = diffRecordMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Persist data.
     * @param record record entity.
     * @return result value.
     */
    @Override
    public DiffRecord save(DiffRecord record) {
        DiffRecordDO dataObject = toDataObject(record);
        diffRecordMapper.insert(dataObject);
        return toDomain(dataObject);
    }

    /**
     * Execute findById business logic.
     * @param id record id.
     * @return result value.
     */
    @Override
    public Optional<DiffRecord> findById(long id) {
        DiffRecordDO dataObject = diffRecordMapper.selectById(id);
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
        List<DiffRecordDO> rows = diffRecordMapper.selectList(wrapper);
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
                dataObject.getCreateTime());
    }

    private String writeDiffItems(List<DiffItem> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception ex) {
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
            throw new IllegalStateException("failed to deserialize diff items", ex);
        }
    }

    private record DiffItemPayload(String fieldPath, String oldValue, String newValue, String diffType) {
    }
}

