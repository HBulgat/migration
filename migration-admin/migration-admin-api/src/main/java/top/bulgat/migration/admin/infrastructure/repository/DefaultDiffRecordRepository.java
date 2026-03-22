package top.bulgat.migration.admin.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import top.bulgat.migration.admin.domain.model.DiffItem;
import top.bulgat.migration.admin.domain.model.DiffRecord;
import top.bulgat.migration.admin.domain.model.DiffStatisticsPoint;
import top.bulgat.migration.admin.domain.repository.DiffRecordRepository;
import top.bulgat.migration.config.common.model.dataobject.DiffRecordDO;
import top.bulgat.migration.config.common.model.enums.DiffType;
import top.bulgat.migration.config.common.dal.DiffRecordDAO;
import top.bulgat.migration.config.common.model.dto.DiffStatisticsQueryResult;

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
            Integer migrationStatus,
            String traceId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int pageSize,
            boolean selectFull) {
        LambdaQueryWrapper<DiffRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DiffRecordDO::getMigrationKey, migrationKey);
        if (hasDiff != null) {
            wrapper.eq(DiffRecordDO::getHasDiff, hasDiff);
        }
        if (migrationStatus != null) {
            wrapper.eq(DiffRecordDO::getMigrationStatus, migrationStatus);
        }
        if (traceId != null && !traceId.isBlank()) {
            wrapper.eq(DiffRecordDO::getTraceId, traceId);
        }
        if (startDate != null) {
            wrapper.ge(DiffRecordDO::getCreateTime, startDate);
        }
        if (endDate != null) {
            wrapper.le(DiffRecordDO::getCreateTime, endDate);
        }
        
        // 字段投影优化
        if (!selectFull) {
            wrapper.select(DiffRecordDO::getId, 
                           DiffRecordDO::getMigrationKey, 
                           DiffRecordDO::getTraceId,
                           DiffRecordDO::getHasDiff, 
                           DiffRecordDO::getDiffType, 
                           DiffRecordDO::getGrayParam,
                           DiffRecordDO::getOldCostTimeMs, 
                           DiffRecordDO::getNewCostTimeMs, 
                           DiffRecordDO::getTotalCostTimeMs,
                           DiffRecordDO::getCreateTime,
                           DiffRecordDO::getOldErrorMessage,
                           DiffRecordDO::getNewErrorMessage,
                           DiffRecordDO::getMigrationStatus,
                           DiffRecordDO::getGrayHit,
                           DiffRecordDO::getFallbackTriggered,
                           DiffRecordDO::getDiffResults);
        }

        wrapper.orderByDesc(DiffRecordDO::getCreateTime);

        // 分页优化
        Page<DiffRecordDO> pageParam =
                new Page<>(page, pageSize);
        List<DiffRecordDO> rows = diffRecordDAO.selectPage(pageParam, wrapper).getRecords();

        log.info("diff_record.findByCondition migrationKey={}, hasDiff={}, status={}, rows={}, selectFull={}",
                migrationKey, hasDiff, migrationStatus, rows.size(), selectFull);

        return rows.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countByCondition(
            String migrationKey,
            Integer hasDiff,
            Integer migrationStatus,
            String traceId,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        LambdaQueryWrapper<DiffRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DiffRecordDO::getMigrationKey, migrationKey);
        if (hasDiff != null) {
            wrapper.eq(DiffRecordDO::getHasDiff, hasDiff);
        }
        if (migrationStatus != null) {
            wrapper.eq(DiffRecordDO::getMigrationStatus, migrationStatus);
        }
        if (traceId != null && !traceId.isBlank()) {
            wrapper.eq(DiffRecordDO::getTraceId, traceId);
        }
        if (startDate != null) {
            wrapper.ge(DiffRecordDO::getCreateTime, startDate);
        }
        if (endDate != null) {
            wrapper.le(DiffRecordDO::getCreateTime, endDate);
        }
        return diffRecordDAO.selectCount(wrapper);
    }

    @Override
    public List<DiffStatisticsPoint> calculateTrendStatistics(
            String migrationKey,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer status,
            int granularitySeconds) {
        List<DiffStatisticsQueryResult> results = diffRecordDAO.selectTrendStatistics(
                migrationKey, startDate, endDate, status, granularitySeconds);
        return results.stream().map(res -> new DiffStatisticsPoint(
                res.getTimePoint(),
                res.getTotalCount(),
                res.getDiffCount(),
                res.getTotalCount() == 0 ? 0 : (double) res.getDiffCount() / res.getTotalCount(),
                res.getAvgOldCost() == null ? 0 : res.getAvgOldCost().intValue(),
                res.getAvgNewCost() == null ? 0 : res.getAvgNewCost().intValue()
        )).collect(Collectors.toList());
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
        dataObject.setGrayParam(record.getGrayParam());
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
        dataObject.setMigrationStatus(record.getMigrationTaskStatus());
        dataObject.setGrayRules(record.getGrayRules());
        dataObject.setGrayHit(record.getGrayHit() != null && record.getGrayHit() ? 1 : 0);
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
