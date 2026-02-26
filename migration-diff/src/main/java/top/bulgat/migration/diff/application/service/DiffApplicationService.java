package top.bulgat.migration.diff.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import top.bulgat.common.exception.BizException;
import top.bulgat.common.exception.ErrorCode;
import top.bulgat.migration.diff.application.command.ExecuteDiffCommand;
import top.bulgat.migration.diff.domain.model.DiffRequest;
import top.bulgat.migration.diff.domain.model.DiffResult;
import top.bulgat.migration.diff.domain.model.DiffRule;
import top.bulgat.migration.diff.domain.repository.DiffRecordRepository;
import top.bulgat.migration.diff.domain.repository.DiffRuleRepository;
import top.bulgat.migration.diff.domain.service.DiffDomainService;

/**
 * Diff应用服务。
 * 负责组装比对请求、拉取规则、调用领域服务并落库比对记录。
 */
@Service
public class DiffApplicationService {

    private final DiffDomainService domainService;
    private final DiffRecordRepository diffRecordRepository;
    private final DiffRuleRepository diffRuleRepository;

    public DiffApplicationService(
            DiffDomainService domainService,
            DiffRecordRepository diffRecordRepository,
            DiffRuleRepository diffRuleRepository) {
        this.domainService = domainService;
        this.diffRecordRepository = diffRecordRepository;
        this.diffRuleRepository = diffRuleRepository;
    }

    /**
     * 执行一次Diff比对用例。
     *
     * @param command Diff执行命令
     * @return Diff结果
     */
    public DiffResult executeDiff(ExecuteDiffCommand command) {
        validateCommand(command);
        DiffRequest request = new DiffRequest(
                command.migrationKey(),
                command.traceId(),
                command.oldJson(),
                command.newJson(),
                command.oldCostTimeMs(),
                command.newCostTimeMs(),
                command.grayscaleParam());
        List<DiffRule> rules = diffRuleRepository.findEnabledRules(command.migrationKey());
        DiffResult result = domainService.execute(request, rules);
        diffRecordRepository.save(request, result);
        return result;
    }

    private void validateCommand(ExecuteDiffCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "diff command is required");
        }
        if (command.migrationKey() == null || command.migrationKey().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "migration_key is required");
        }
        if (command.migrationKey().length() > 128) {
            throw new BizException(ErrorCode.PARAM_ERROR, "migration_key is too long");
        }
        if (command.migrationKey().chars().anyMatch(Character::isWhitespace)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "migration_key must not contain space");
        }
        if (command.oldJson() == null || command.oldJson().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "old_json is required");
        }
        if (command.newJson() == null || command.newJson().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "new_json is required");
        }
        if (command.oldCostTimeMs() != null && command.oldCostTimeMs() < 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "old_cost_time_ms must be greater than or equal to 0");
        }
        if (command.newCostTimeMs() != null && command.newCostTimeMs() < 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "new_cost_time_ms must be greater than or equal to 0");
        }
    }
}
