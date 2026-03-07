package top.bulgat.migration.admin.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import top.bulgat.common.base.exception.BizException;
import top.bulgat.common.base.exception.ErrorCode;
import top.bulgat.migration.admin.application.command.CreateGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.DeleteGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.ListGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.QueryMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.UpdateGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.UpdateGrayscaleRuleEnableCommand;
import top.bulgat.migration.admin.domain.model.GrayscaleRule;
import top.bulgat.migration.admin.domain.model.GrayscaleRuleType;
import top.bulgat.migration.admin.domain.repository.GrayscaleRuleRepository;
import top.bulgat.migration.admin.domain.service.GrayscaleRuleDomainService;
import top.bulgat.migration.admin.domain.service.MigrationTaskDomainService;

/**
 * 灰度规则应用服务。
 * 负责灰度规则新增、更新、启停和分页查询等用例编排。
 */
@Service
public class GrayscaleRuleApplicationService {

    private final MigrationTaskApplicationService migrationTaskApplicationService;
    private final GrayscaleRuleRepository repository;
    private final GrayscaleRuleDomainService domainService;
    private final MigrationTaskDomainService migrationTaskDomainService;

    public GrayscaleRuleApplicationService(
            MigrationTaskApplicationService migrationTaskApplicationService,
            GrayscaleRuleRepository repository,
            GrayscaleRuleDomainService domainService,
            MigrationTaskDomainService migrationTaskDomainService) {
        this.migrationTaskApplicationService = migrationTaskApplicationService;
        this.repository = repository;
        this.domainService = domainService;
        this.migrationTaskDomainService = migrationTaskDomainService;
    }

    /**
     * 创建灰度规则。
     *
     * @param command 创建命令
     * @return 已保存的灰度规则
     */
    public GrayscaleRule create(CreateGrayscaleRuleCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "create command is required");
        }
        return doCreate(command.migrationKey(), command.ruleType(), command.ruleValue(), command.enable());
    }

    /**
     * 更新灰度规则。
     * 支持部分字段更新，未传字段保持原值。
     *
     * @param command 更新命令
     */
    public void update(UpdateGrayscaleRuleCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "update command is required");
        }
        doUpdate(
                command.migrationKey(),
                command.ruleId(),
                command.ruleType(),
                command.ruleValue(),
                command.enable());
    }

    /**
     * 删除灰度规则。
     *
     * @param command 删除命令
     */
    public void delete(DeleteGrayscaleRuleCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "delete command is required");
        }
        doDelete(command.migrationKey(), command.ruleId());
    }

    /**
     * 更新灰度规则启用状态。
     *
     * @param command 启停更新命令
     */
    public void updateEnable(UpdateGrayscaleRuleEnableCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "update_enable command is required");
        }
        doUpdateEnable(command.migrationKey(), command.ruleId(), command.enable());
    }

    /**
     * 分页查询灰度规则列表。
     *
     * @param command 查询命令
     * @return 当前页灰度规则
     */
    public List<GrayscaleRule> list(ListGrayscaleRuleCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "list command is required");
        }
        return doList(command.migrationKey(), command.page(), command.pageSize());
    }

    /**
     * 查询指定迁移任务下的全部灰度规则，供 SDK 配置读取使用。
     *
     * @param migrationKey 迁移标识
     * @return 全量灰度规则
     */
    public List<GrayscaleRule> listAllByMigrationKey(String migrationKey) {
        validateMigrationKey(migrationKey);
        return doListAll(migrationKey);
    }

    /**
     * 统计指定迁移任务下的灰度规则总数。
     *
     * @param command 查询命令
     * @return 规则总数
     */
    public long count(ListGrayscaleRuleCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "count command is required");
        }
        return doCount(command.migrationKey());
    }

    private GrayscaleRule doCreate(String migrationKey, String ruleType, String ruleValue, boolean enable) {
        try {
            validateMigrationKey(migrationKey);
            migrationTaskApplicationService.getByMigrationKey(new QueryMigrationTaskCommand(migrationKey));
            GrayscaleRule rule = new GrayscaleRule(
                    UUID.randomUUID().toString().replace("-", ""),
                    migrationKey,
                    GrayscaleRuleType.fromValue(ruleType),
                    ruleValue,
                    enable);
            domainService.validateRule(rule);
            return repository.save(rule);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, ex.getMessage());
        }
    }

    private void doUpdate(String migrationKey, String ruleId, String ruleType, String ruleValue, Boolean enable) {
        validateMigrationKey(migrationKey);
        if (ruleType == null && ruleValue == null && enable == null) {
            throw new BizException(
                    ErrorCode.PARAM_ERROR,
                    "at least one field(rule_type/rule_value/enable) must be provided");
        }
        try {
            GrayscaleRule existing = getByMigrationKeyAndRuleId(migrationKey, ruleId);
            GrayscaleRuleType targetType = ruleType == null ? null : GrayscaleRuleType.fromValue(ruleType);
            if (targetType != null || ruleValue != null) {
                domainService.validateRuleValue(
                        targetType == null ? existing.getRuleType() : targetType,
                        ruleValue == null ? existing.getRuleValue() : ruleValue);
            }
            existing.update(targetType, ruleValue, enable);
            repository.save(existing);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, ex.getMessage());
        }
    }

    private void doDelete(String migrationKey, String ruleId) {
        validateMigrationKey(migrationKey);
        getByMigrationKeyAndRuleId(migrationKey, ruleId);
        repository.deleteByMigrationKeyAndRuleId(migrationKey, ruleId);
    }

    private void doUpdateEnable(String migrationKey, String ruleId, boolean enable) {
        validateMigrationKey(migrationKey);
        GrayscaleRule existing = getByMigrationKeyAndRuleId(migrationKey, ruleId);
        existing.updateEnable(enable);
        repository.save(existing);
    }

    private List<GrayscaleRule> doList(String migrationKey, int page, int pageSize) {
        validateMigrationKey(migrationKey);
        validatePagination(page, pageSize);
        return doListAll(migrationKey).stream()
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
    }

    private long doCount(String migrationKey) {
        validateMigrationKey(migrationKey);
        return repository.findByMigrationKey(migrationKey).size();
    }

    private List<GrayscaleRule> doListAll(String migrationKey) {
        return repository.findByMigrationKey(migrationKey).stream()
                .sorted(Comparator.comparing(GrayscaleRule::getUpdateTime).reversed())
                .collect(Collectors.toList());
    }

    private GrayscaleRule getByMigrationKeyAndRuleId(String migrationKey, String ruleId) {
        return repository.findByMigrationKeyAndRuleId(migrationKey, ruleId)
                .orElseThrow(() -> new BizException(
                        ErrorCode.NOT_FOUND,
                        "grayscale rule not found: " + ruleId));
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
