package top.bulgat.migration.diff.infrastructure.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import top.bulgat.migration.diff.domain.model.DiffItem;
import top.bulgat.migration.diff.domain.model.DiffRecord;
import top.bulgat.migration.diff.domain.model.DiffRequest;
import top.bulgat.migration.diff.domain.model.DiffResult;
import top.bulgat.migration.diff.domain.model.DiffType;
import top.bulgat.migration.diff.domain.repository.DiffRecordRepository;
import top.bulgat.migration.diff.infrastructure.persistence.entity.DiffRecordDO;
import top.bulgat.migration.diff.infrastructure.persistence.mapper.DiffRecordMapper;

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
     * @param request request payload.
     * @param result result object.
     * @return result value.
     */
    @Override
    public DiffRecord save(DiffRequest request, DiffResult result) {
        String diffType = result.getDiffItems().isEmpty()
                ? null
                : result.getDiffItems().get(0).getDiffType().name();
        DiffRecordDO dataObject = new DiffRecordDO();
        dataObject.setMigrationKey(request.getMigrationKey());
        dataObject.setTraceId(request.getTraceId());
        dataObject.setOldResponse(request.getOldJson());
        dataObject.setNewResponse(request.getNewJson());
        dataObject.setDiffResults(writeDiffItems(result.getDiffItems()));
        dataObject.setHasDiff(result.hasDiff() ? 1 : 0);
        dataObject.setDiffType(diffType);
        dataObject.setGrayscaleParam(request.getGrayscaleParam());
        dataObject.setOldCostTimeMs(request.getOldCostTimeMs());
        dataObject.setNewCostTimeMs(request.getNewCostTimeMs());
        dataObject.setTotalCostTimeMs((int) result.getCostTimeMs());
        dataObject.setCreateTime(LocalDateTime.now());
        diffRecordMapper.insert(dataObject);
        return toDomain(dataObject);
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

    private record DiffItemPayload(String fieldPath, String oldValue, String newValue, String diffType) {
    }
}

