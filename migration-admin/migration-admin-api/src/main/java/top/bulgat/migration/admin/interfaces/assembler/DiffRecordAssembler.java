package top.bulgat.migration.admin.interfaces.assembler;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import top.bulgat.migration.admin.application.command.CountDiffRecordCommand;
import top.bulgat.migration.admin.application.command.DetailDiffRecordCommand;
import top.bulgat.migration.admin.application.command.ListDiffRecordCommand;
import top.bulgat.migration.admin.application.command.StatisticsDiffRecordCommand;
import top.bulgat.migration.admin.application.service.DiffRecordQueryApplicationService;
import top.bulgat.migration.admin.domain.model.DiffItem;
import top.bulgat.migration.admin.domain.model.DiffRecord;
import top.bulgat.migration.admin.interfaces.dto.DiffItemResponse;
import top.bulgat.migration.admin.interfaces.dto.DiffRecordResponse;
import top.bulgat.migration.admin.interfaces.dto.DiffStatisticsResponse;

/**
 * DiffRecordAssembler converts DTOs and domain models.
 */
@Component
public class DiffRecordAssembler {



    /**
     * Execute toListCommand business logic.
     * @param migrationKey migration key.
     * @param hasDiff has-diff filter.
     * @param startDate start date.
     * @param endDate end date.
     * @param page page index.
     * @param pageSize page size.
     * @return result value.
     */
    public ListDiffRecordCommand toListCommand(
            String migrationKey,
            Integer hasDiff,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int pageSize) {
        return new ListDiffRecordCommand(migrationKey, hasDiff, startDate, endDate, page, pageSize);
    }

    /**
     * Execute toCountCommand business logic.
     * @param migrationKey migration key.
     * @param hasDiff has-diff filter.
     * @param startDate start date.
     * @param endDate end date.
     * @return result value.
     */
    public CountDiffRecordCommand toCountCommand(
            String migrationKey,
            Integer hasDiff,
            LocalDate startDate,
            LocalDate endDate) {
        return new CountDiffRecordCommand(migrationKey, hasDiff, startDate, endDate);
    }

    /**
     * Execute toDetailCommand business logic.
     * @param id record id.
     * @return result value.
     */
    public DetailDiffRecordCommand toDetailCommand(long id) {
        return new DetailDiffRecordCommand(id);
    }

    /**
     * Execute toStatisticsCommand business logic.
     * @param migrationKey migration key.
     * @param startDate start date.
     * @param endDate end date.
     * @return result value.
     */
    public StatisticsDiffRecordCommand toStatisticsCommand(
            String migrationKey,
            LocalDate startDate,
            LocalDate endDate) {
        return new StatisticsDiffRecordCommand(migrationKey, startDate, endDate);
    }

    /**
     * Execute toResponse business logic.
     * @param record record entity.
     * @return result value.
     */
    public DiffRecordResponse toResponse(DiffRecord record) {
        return new DiffRecordResponse(
                record.getId(),
                record.getMigrationKey(),
                record.getTraceId(),
                record.getOldResponse(),
                record.getNewResponse(),
                toItemResponse(record.getDiffResults()),
                record.isHasDiff(),
                record.getDiffType(),
                record.getGrayscaleParam(),
                record.getOldCostTimeMs(),
                record.getNewCostTimeMs(),
                record.getTotalCostTimeMs(),
                record.getCreateTime());
    }

    /**
     * Execute toResponseList business logic.
     * @param records method parameter.
     * @return result value.
     */
    public List<DiffRecordResponse> toResponseList(List<DiffRecord> records) {
        return records.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Execute toStatisticsResponse business logic.
     * @param statistics method parameter.
     * @return result value.
     */
    public DiffStatisticsResponse toStatisticsResponse(DiffRecordQueryApplicationService.DiffStatistics statistics) {
        return new DiffStatisticsResponse(
                statistics.totalCount(),
                statistics.diffCount(),
                statistics.diffRate(),
                statistics.avgOldCostTime(),
                statistics.avgNewCostTime());
    }

    private List<DiffItemResponse> toItemResponse(List<DiffItem> items) {
        return items.stream()
                .map(item -> new DiffItemResponse(
                        item.getFieldPath(),
                        item.getOldValue(),
                        item.getNewValue(),
                        item.getDiffType().name()))
                .collect(Collectors.toList());
    }
}

