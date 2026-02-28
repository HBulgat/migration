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
 * DiffRecordAssembler 用于转换DTO和领域模型。
 */
@Component
public class DiffRecordAssembler {



    /**
     * 执行 toListCommand 业务逻辑。
     * @param migrationKey migration key.
     * @param hasDiff has-diff filter.
     * @param startDate start date.
     * @param endDate end date.
     * @param page 页码。
     * @param pageSize 每页大小。
     * @return 返回结果。
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
     * 执行 toCountCommand 业务逻辑。
     * @param migrationKey migration key.
     * @param hasDiff has-diff filter.
     * @param startDate start date.
     * @param endDate end date.
     * @return 返回结果。
     */
    public CountDiffRecordCommand toCountCommand(
            String migrationKey,
            Integer hasDiff,
            LocalDate startDate,
            LocalDate endDate) {
        return new CountDiffRecordCommand(migrationKey, hasDiff, startDate, endDate);
    }

    /**
     * 执行 toDetailCommand 业务逻辑。
     * @param id record id.
     * @return 返回结果。
     */
    public DetailDiffRecordCommand toDetailCommand(long id) {
        return new DetailDiffRecordCommand(id);
    }

    /**
     * 执行 toStatisticsCommand 业务逻辑。
     * @param migrationKey migration key.
     * @param startDate start date.
     * @param endDate end date.
     * @return 返回结果。
     */
    public StatisticsDiffRecordCommand toStatisticsCommand(
            String migrationKey,
            LocalDate startDate,
            LocalDate endDate) {
        return new StatisticsDiffRecordCommand(migrationKey, startDate, endDate);
    }

    /**
     * 执行 toResponse 业务逻辑。
     * @param record record entity.
     * @return 返回结果。
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
     * 执行 toResponseList 业务逻辑。
     * @param records 方法参数。
     * @return 返回结果。
     */
    public List<DiffRecordResponse> toResponseList(List<DiffRecord> records) {
        return records.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 执行 toStatisticsResponse 业务逻辑。
     * @param statistics 方法参数。
     * @return 返回结果。
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

