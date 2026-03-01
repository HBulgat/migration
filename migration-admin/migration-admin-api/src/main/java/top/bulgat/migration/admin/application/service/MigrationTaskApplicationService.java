package top.bulgat.migration.admin.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import top.bulgat.common.base.exception.BizException;
import top.bulgat.common.base.exception.ErrorCode;
import top.bulgat.migration.admin.application.command.CreateMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.DeleteMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.ListMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.QueryMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.UpdateMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.UpdateMigrationTaskStatusCommand;
import top.bulgat.migration.admin.domain.model.MigrationStatus;
import top.bulgat.migration.admin.domain.model.MigrationTask;
import top.bulgat.migration.admin.domain.repository.DiffRuleRepository;
import top.bulgat.migration.admin.domain.repository.GrayscaleRuleRepository;
import top.bulgat.migration.admin.domain.repository.MigrationTaskRepository;
import top.bulgat.migration.admin.domain.service.MigrationTaskDomainService;

/**
 * 迁移任务应用服务。
 * 负责编排任务创建、状态更新、查询与删除等用例，不承载基础设施细节。
 */
@Service
public class MigrationTaskApplicationService {

    private final MigrationTaskRepository repository;
    private final GrayscaleRuleRepository grayscaleRuleRepository;
    private final DiffRuleRepository diffRuleRepository;
    private final MigrationTaskDomainService domainService;

    public MigrationTaskApplicationService(
            MigrationTaskRepository repository,
            GrayscaleRuleRepository grayscaleRuleRepository,
            DiffRuleRepository diffRuleRepository,
            MigrationTaskDomainService domainService) {
        this.repository = repository;
        this.grayscaleRuleRepository = grayscaleRuleRepository;
        this.diffRuleRepository = diffRuleRepository;
        this.domainService = domainService;
    }

    /**
     * 创建迁移任务。
     *
     * @param command 创建命令
     * @return 已持久化的迁移任务
     */
    public MigrationTask createMigrationTask(CreateMigrationTaskCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "create command is required");
        }
        try {
            domainService.validateMigrationKey(command.migrationKey());
            domainService.validateDescription(command.description());
            MigrationTask task = new MigrationTask(
                    command.migrationKey(),
                    MigrationStatus.fromCode(command.status()),
                    command.description());
            domainService.validateForCreation(task, repository);
            return repository.save(task);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, ex.getMessage());
        }
    }

    /**
     * 更新迁移任务信息。
     * 支持按需更新状态和描述，未传字段保持原值。
     *
     * @param command 更新命令
     * @return 更新后的迁移任务
     */
    public MigrationTask updateTask(UpdateMigrationTaskCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "update command is required");
        }
        return doUpdateTask(command.migrationKey(), command.status(), command.description());
    }

    /**
     * 单独更新迁移任务状态。
     *
     * @param command 状态更新命令
     * @return 更新后的迁移任务
     */
    public MigrationTask updateStatus(UpdateMigrationTaskStatusCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "update_status command is required");
        }
        return doUpdateStatus(command.migrationKey(), command.targetStatus());
    }

    /**
     * 按 migrationKey 查询迁移任务。
     *
     * @param command 查询命令
     * @return 匹配到的迁移任务
     */
    public MigrationTask getByMigrationKey(QueryMigrationTaskCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "query command is required");
        }
        return doGetByMigrationKey(command.migrationKey());
    }

    /**
     * 删除迁移任务及其关联灰度规则。
     *
     * @param command 删除命令
     */
    public void deleteByMigrationKey(DeleteMigrationTaskCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "delete command is required");
        }
        doDeleteByMigrationKey(command.migrationKey());
    }

    /**
     * 分页查询迁移任务列表。
     *
     * @param command 查询命令
     * @return 当前页任务列表
     */
    public List<MigrationTask> list(ListMigrationTaskCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "list command is required");
        }
        return doList(command.status(), command.keyword(), command.page(), command.pageSize());
    }

    /**
     * 统计符合筛选条件的迁移任务总数。
     *
     * @param command 查询命令
     * @return 任务总数
     */
    public long count(ListMigrationTaskCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "count command is required");
        }
        return doCount(command.status(), command.keyword());
    }

    /**
     * 判断迁移任务是否存在。
     *
     * @param migrationKey 迁移任务标识
     * @return 是否存在
     */
    public boolean existsByMigrationKey(String migrationKey) {
        return repository.existsByMigrationKey(migrationKey);
    }

    private MigrationTask doUpdateTask(String migrationKey, Integer targetStatus, String description) {
        if (targetStatus == null && description == null) {
            throw new BizException(
                    ErrorCode.PARAM_ERROR,
                    "at least one field(status/description) must be provided");
        }
        try {
            MigrationTask task = doGetByMigrationKey(migrationKey);
            if (description != null) {
                domainService.validateDescription(description);
                task.updateDescription(description);
            }
            if (targetStatus != null) {
                MigrationStatus target = MigrationStatus.fromCode(targetStatus);
                domainService.validateStatusSwitch(task.getStatus(), target);
                task.changeStatus(target);
            }
            return repository.save(task);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, ex.getMessage());
        }
    }

    private MigrationTask doUpdateStatus(String migrationKey, int targetStatus) {
        try {
            MigrationTask task = doGetByMigrationKey(migrationKey);
            MigrationStatus target = MigrationStatus.fromCode(targetStatus);
            domainService.validateStatusSwitch(task.getStatus(), target);
            task.changeStatus(target);
            return repository.save(task);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, ex.getMessage());
        }
    }

    private MigrationTask doGetByMigrationKey(String migrationKey) {
        try {
            domainService.validateMigrationKey(migrationKey);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, ex.getMessage());
        }
        return repository.findByMigrationKey(migrationKey)
                .orElseThrow(() -> new BizException(
                        ErrorCode.NOT_FOUND,
                        "migration task not found: " + migrationKey));
    }

    private void doDeleteByMigrationKey(String migrationKey) {
        doGetByMigrationKey(migrationKey);
        repository.deleteByMigrationKey(migrationKey);
        grayscaleRuleRepository.deleteByMigrationKey(migrationKey);
        diffRuleRepository.deleteByMigrationKey(migrationKey);
    }

    private List<MigrationTask> doList(Integer status, String keyword, int page, int pageSize) {
        validatePagination(page, pageSize);
        validateStatusFilter(status);
        return repository.findAll().stream()
                .filter(task -> status == null || task.getStatus().getCode() == status)
                .filter(task -> keyword == null || keyword.isBlank() || task.getMigrationKey().contains(keyword))
                .sorted(Comparator.comparing(MigrationTask::getUpdateTime).reversed())
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
    }

    private long doCount(Integer status, String keyword) {
        validateStatusFilter(status);
        return repository.findAll().stream()
                .filter(task -> status == null || task.getStatus().getCode() == status)
                .filter(task -> keyword == null || keyword.isBlank() || task.getMigrationKey().contains(keyword))
                .count();
    }

    private void validateStatusFilter(Integer status) {
        if (status == null) {
            return;
        }
        if (status < 1 || status > 7) {
            throw new BizException(ErrorCode.PARAM_ERROR, "status out of range [1,7]");
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
}
