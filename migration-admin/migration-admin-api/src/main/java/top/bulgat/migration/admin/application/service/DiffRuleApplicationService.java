package top.bulgat.migration.admin.application.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import top.bulgat.common.exception.BizException;
import top.bulgat.common.exception.ErrorCode;
import top.bulgat.migration.admin.application.command.CreateDiffRuleCommand;
import top.bulgat.migration.admin.application.command.DeleteDiffRuleCommand;
import top.bulgat.migration.admin.application.command.ListDiffRuleCommand;
import top.bulgat.migration.admin.application.command.UpdateDiffRuleCommand;
import top.bulgat.migration.admin.application.command.UpdateDiffRuleEnableCommand;
import top.bulgat.migration.admin.domain.model.DiffRule;
import top.bulgat.migration.admin.domain.model.DiffRuleType;
import top.bulgat.migration.admin.domain.repository.DiffRuleRepository;
import top.bulgat.migration.admin.domain.repository.MigrationTaskRepository;
import top.bulgat.migration.admin.domain.service.DiffRuleDomainService;

/**
 * Diff规则应用服务
 */
@Service
public class DiffRuleApplicationService {

    private final DiffRuleRepository repository;
    private final MigrationTaskRepository taskRepository;
    private final DiffRuleDomainService domainService;

    public DiffRuleApplicationService(
            DiffRuleRepository repository,
            MigrationTaskRepository taskRepository,
            DiffRuleDomainService domainService) {
        this.repository = repository;
        this.taskRepository = taskRepository;
        this.domainService = domainService;
    }

    public DiffRule create(CreateDiffRuleCommand command) {
        if (!taskRepository.existsByMigrationKey(command.migrationKey())) {
            throw new BizException(ErrorCode.NOT_FOUND, "migration task not found");
        }
        DiffRuleType ruleType = DiffRuleType.fromValue(command.ruleType());
        domainService.validateRule(ruleType, command.fieldPath(), command.ruleValue());

        DiffRule rule = new DiffRule(
                command.migrationKey(),
                UUID.randomUUID().toString().replace("-", ""),
                ruleType,
                command.fieldPath(),
                command.ruleValue(),
                command.enable(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        return repository.save(rule);
    }

    public void update(UpdateDiffRuleCommand command) {
        if (!taskRepository.existsByMigrationKey(command.migrationKey())) {
            throw new BizException(ErrorCode.NOT_FOUND, "migration task not found");
        }
        DiffRule rule = getRule(command.migrationKey(), command.ruleId());
        
        DiffRuleType newType = command.ruleType() != null ? DiffRuleType.fromValue(command.ruleType()) : rule.getRuleType();
        String newPath = command.fieldPath() != null ? command.fieldPath() : rule.getFieldPath();
        String newValue = command.ruleValue() != null ? command.ruleValue() : rule.getRuleValue();
        
        domainService.validateRule(newType, newPath, newValue);
        rule.update(newType, newPath, newValue, command.enable());
        repository.save(rule);
    }

    public void updateEnable(UpdateDiffRuleEnableCommand command) {
        if (!taskRepository.existsByMigrationKey(command.migrationKey())) {
            throw new BizException(ErrorCode.NOT_FOUND, "migration task not found");
        }
        DiffRule rule = getRule(command.migrationKey(), command.ruleId());
        rule.changeEnable(command.enable());
        repository.save(rule);
    }

    public void delete(DeleteDiffRuleCommand command) {
        if (!taskRepository.existsByMigrationKey(command.migrationKey())) {
            throw new BizException(ErrorCode.NOT_FOUND, "migration task not found");
        }
        repository.deleteByRuleId(command.migrationKey(), command.ruleId());
    }

    public List<DiffRule> list(ListDiffRuleCommand command) {
        if (!taskRepository.existsByMigrationKey(command.migrationKey())) {
            throw new BizException(ErrorCode.NOT_FOUND, "migration task not found");
        }
        if (command.page() < 1 || command.pageSize() < 1 || command.pageSize() > 200) {
            throw new BizException(ErrorCode.PARAM_ERROR, "invalid page or page_size");
        }

        List<DiffRule> rules = repository.findByMigrationKey(command.migrationKey());
        return rules.stream()
                .sorted(Comparator.comparing(DiffRule::getCreateTime).reversed())
                .skip((long) (command.page() - 1) * command.pageSize())
                .limit(command.pageSize())
                .collect(Collectors.toList());
    }

    public long count(ListDiffRuleCommand command) {
        if (!taskRepository.existsByMigrationKey(command.migrationKey())) {
            throw new BizException(ErrorCode.NOT_FOUND, "migration task not found");
        }
        return repository.findByMigrationKey(command.migrationKey()).size();
    }

    private DiffRule getRule(String migrationKey, String ruleId) {
        return repository.findByMigrationKey(migrationKey).stream()
                .filter(r -> r.getRuleId().equals(ruleId))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "diff rule not found"));
    }
}
