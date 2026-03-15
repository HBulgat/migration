package top.bulgat.migration.admin.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.bulgat.common.base.exception.BizException;
import top.bulgat.common.base.exception.ErrorCode;
import top.bulgat.migration.admin.application.command.CountDiffRecordCommand;
import top.bulgat.migration.admin.application.command.DetailDiffRecordCommand;
import top.bulgat.migration.admin.application.command.ListDiffRecordCommand;
import top.bulgat.migration.admin.application.command.StatisticsDiffRecordCommand;
import top.bulgat.migration.admin.domain.model.DiffRecord;
import top.bulgat.migration.admin.domain.model.DiffStatisticsPoint;
import top.bulgat.migration.admin.domain.model.StatisticsGranularity;
import top.bulgat.migration.admin.domain.repository.DiffRecordRepository;
import top.bulgat.migration.admin.domain.service.MigrationTaskDomainService;

/**
 * Diff记录查询应用服务。
 * 面向管理端提供分页查询、详情查询和统计查询能力。
 */
@Service
public class DiffRecordQueryApplicationService {

    private static final Logger log = LoggerFactory.getLogger(DiffRecordQueryApplicationService.class);
    private final DiffRecordRepository repository;
    private final MigrationTaskDomainService migrationTaskDomainService;

    public DiffRecordQueryApplicationService(
            DiffRecordRepository repository,
            MigrationTaskDomainService migrationTaskDomainService) {
        this.repository = repository;
        this.migrationTaskDomainService = migrationTaskDomainService;
    }

    /**
     * 分页查询Diff记录。
     *
     * @param command 查询命令
     * @return 当前页Diff记录
     */
    public List<DiffRecord> list(ListDiffRecordCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "list command is required");
        }
        log.info("diff_record.list start migrationKey={}, hasDiff={}, migrationStatus={}, startDate={}, endDate={}, page={}, pageSize={}",
                command.migrationKey(), command.hasDiff(), command.migrationStatus(), command.startDate(), command.endDate(), command.page(),
                command.pageSize());
        return doList(
                command.migrationKey(),
                command.hasDiff(),
                command.migrationStatus(),
                command.traceId(),
                command.startDate(),
                command.endDate(),
                command.page(),
                command.pageSize());
    }

    /**
     * 统计符合筛选条件的Diff记录总数。
     *
     * @param command 查询命令
     * @return 记录总数
     */
    public long count(CountDiffRecordCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "count command is required");
        }
        log.info("diff_record.count start migrationKey={}, hasDiff={}, migrationStatus={}, traceId={}, startDate={}, endDate={}",
                command.migrationKey(), command.hasDiff(), command.migrationStatus(), command.traceId(), command.startDate(), command.endDate());
        return doCount(command.migrationKey(), command.hasDiff(), command.migrationStatus(), command.traceId(), command.startDate(), command.endDate());
    }

    /**
     * 查询单条Diff记录详情。
     *
     * @param command 明细查询命令
     * @return Diff记录详情
     */
    public DiffRecord detail(DetailDiffRecordCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "detail command is required");
        }
        log.info("diff_record.detail start id={}", command.id());
        return doDetail(command.id());
    }

    /**
     * 统计Diff时序数据。
     *
     * @param command 统计查询命令
     * @return 统计结果列表
     */
    public List<DiffStatisticsPoint> statistics(StatisticsDiffRecordCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "statistics command is required");
        }
        log.info("diff_record.statistics start migrationKey={}, startDate={}, endDate={}, status={}, granularity={}",
                command.migrationKey(), command.startDate(), command.endDate(), command.migrationStatus(), command.granularity());
        return doStatistics(command.migrationKey(), command.startDate(), command.endDate(), command.migrationStatus(), command.granularity());
    }

    private List<DiffRecord> doList(
            String migrationKey,
            Integer hasDiff,
            Integer migrationStatus,
            String traceId,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int pageSize) {
        validateMigrationKey(migrationKey);
        validatePagination(page, pageSize);
        validateHasDiffFilter(hasDiff);
        validateDateRange(startDate, endDate);
        
        List<DiffRecord> records = repository.findByCondition(
                        migrationKey,
                        hasDiff,
                        migrationStatus,
                        traceId,
                        startDate == null ? null : startDate.atStartOfDay(),
                        endDate == null ? null : endDate.atTime(LocalTime.MAX),
                        page,
                        pageSize,
                        false); // 列表查询不含大字段
        
        log.info("diff_record.list done migrationKey={}, page={}, pageSize={}, resultSize={}",
                migrationKey, page, pageSize, records.size());
        return records;
    }

    private long doCount(String migrationKey, Integer hasDiff, Integer migrationStatus, String traceId, LocalDate startDate, LocalDate endDate) {
        validateMigrationKey(migrationKey);
        validateHasDiffFilter(hasDiff);
        validateDateRange(startDate, endDate);
        long total = repository.countByCondition(
                        migrationKey,
                        hasDiff,
                        migrationStatus,
                        traceId,
                        startDate == null ? null : startDate.atStartOfDay(),
                        endDate == null ? null : endDate.atTime(LocalTime.MAX));
        log.info("diff_record.count done migrationKey={}, total={}", migrationKey, total);
        return total;
    }

    private DiffRecord doDetail(long id) {
        DiffRecord record = repository.findById(id)
                .orElseThrow(() -> new BizException(
                        ErrorCode.NOT_FOUND,
                        "diff record not found: " + id));
        log.info("diff_record.detail done id={}, migrationKey={}, hasDiff={}",
                record.getId(), record.getMigrationKey(), record.isHasDiff());
        return record;
    }

    private List<DiffStatisticsPoint> doStatistics(String migrationKey, LocalDateTime startDate, LocalDateTime endDate, Integer status, StatisticsGranularity granularity) {
        validateMigrationKey(migrationKey);
        StatisticsGranularity targetGranularity = granularity != null ? granularity : StatisticsGranularity.HOUR;
        List<DiffStatisticsPoint> points = repository.calculateTrendStatistics(
                migrationKey,
                startDate,
                endDate,
                status,
                targetGranularity.getSeconds());
        return points;
    }


    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "start_date must not be later than end_date");
        }
    }

    private void validateHasDiffFilter(Integer hasDiff) {
        if (hasDiff == null) {
            return;
        }
        if (hasDiff != 0 && hasDiff != 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "has_diff must be 0 or 1");
        }
    }

    private void validatePagination(int page, int pageSize) {
        if (page < 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "page must be greater than or equal to 1");
        }
        if (pageSize < 1 || pageSize > 200) {
            throw new BizException(ErrorCode.PARAM_ERROR, "pageSize out of range [1,200]");
        }
    }

    private void validateMigrationKey(String migrationKey) {
        try {
            migrationTaskDomainService.validateMigrationKey(migrationKey);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, ex.getMessage());
        }
    }

}
