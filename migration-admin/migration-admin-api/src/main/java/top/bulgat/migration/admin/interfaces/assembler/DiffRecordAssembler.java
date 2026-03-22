package top.bulgat.migration.admin.interfaces.assembler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import top.bulgat.migration.admin.application.command.CountDiffRecordCommand;
import top.bulgat.migration.admin.application.command.DetailDiffRecordCommand;
import top.bulgat.migration.admin.application.command.ListDiffRecordCommand;
import top.bulgat.migration.admin.application.command.StatisticsDiffRecordCommand;
import top.bulgat.migration.admin.domain.model.DiffItem;
import top.bulgat.migration.admin.domain.model.DiffRecord;
import top.bulgat.migration.admin.domain.model.DiffStatisticsPoint;
import top.bulgat.migration.admin.domain.model.StatisticsGranularity;
import top.bulgat.migration.admin.interfaces.dto.DiffItemResponse;
import top.bulgat.migration.admin.interfaces.dto.DiffRecordResponse;
import top.bulgat.migration.admin.interfaces.dto.DiffStatisticsResponse;

/**
 * 用于转换 DTO 与领域模型。
 */
@Component
public class DiffRecordAssembler {



    /**
     * 执行 toListCommand 业务逻辑。
     * @param migrationKey 迁移标识。
     * @param hasDiff 是否有差异过滤条件。
     * @param startDate 开始时间。
     * @param endDate 结束时间。
     * @param page 页码。
     * @param pageSize 每页大小。
     * @return 返回结果。
     */
    public ListDiffRecordCommand toListCommand(
            String migrationKey,
            Integer hasDiff,
            Integer migrationStatus,
            String traceId,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int pageSize) {
        return new ListDiffRecordCommand(migrationKey, hasDiff, migrationStatus, traceId, startDate, endDate, page, pageSize);
    }

    /**
     * 执行 toCountCommand 业务逻辑。
     * @param migrationKey 迁移标识。
     * @param hasDiff 是否有差异过滤条件。
     * @param startDate 开始时间。
     * @param endDate 结束时间。
     * @return 返回结果。
     */
    public CountDiffRecordCommand toCountCommand(
            String migrationKey,
            Integer hasDiff,
            Integer migrationStatus,
            String traceId,
            LocalDate startDate,
            LocalDate endDate) {
        return new CountDiffRecordCommand(migrationKey, hasDiff, migrationStatus, traceId, startDate, endDate);
    }

    /**
     * 执行 toDetailCommand 业务逻辑。
     * @param id 记录 ID。
     * @return 返回结果。
     */
    public DetailDiffRecordCommand toDetailCommand(long id) {
        return new DetailDiffRecordCommand(id);
    }

    /**
     * 执行 toStatisticsCommand 业务逻辑。
     * @param migrationKey 迁移标识。
     * @param startDate 开始时间。
     * @param endDate 结束时间。
     * @param granularity 粒度。
     * @return 返回结果。
     */
    public StatisticsDiffRecordCommand toStatisticsCommand(String migrationKey, LocalDateTime startDate, LocalDateTime endDate, Integer migrationStatus, String granularity) {
        return new StatisticsDiffRecordCommand(migrationKey, startDate, endDate, migrationStatus, StatisticsGranularity.of(granularity));
    }

    /**
     * 执行 toResponse 业务逻辑。
     * @param record 记录实体。
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
                record.getGrayParam(),
                record.getOldCostTimeMs(),
                record.getNewCostTimeMs(),
                record.getTotalCostTimeMs(),
                record.getMigrationTaskStatus(),
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
     * 将领域时序点列表转换为响应 DTO。
     * @param points 统计点列表
     * @return 响应 DTO
     */
    public DiffStatisticsResponse toStatisticsResponse(List<DiffStatisticsPoint> points, StatisticsGranularity granularity) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(granularity.getFormat());
        List<DiffStatisticsResponse.DiffStatisticsPointResponse> itemResponses = points.stream()
                .map(p -> new DiffStatisticsResponse.DiffStatisticsPointResponse(
                        p.timePoint().format(formatter),
                        p.totalCount(),
                        p.diffCount(),
                        p.diffRate(),
                        p.avgOldCostTime(),
                        p.avgNewCostTime()))
                .collect(Collectors.toList());
        return new DiffStatisticsResponse(itemResponses);
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

