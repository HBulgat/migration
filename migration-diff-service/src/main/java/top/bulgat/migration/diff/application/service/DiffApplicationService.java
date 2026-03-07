package top.bulgat.migration.diff.application.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.bulgat.common.base.exception.BizException;
import top.bulgat.common.base.exception.ErrorCode;
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

    private static final Logger log = LoggerFactory.getLogger(DiffApplicationService.class);
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
     * @param command Diff 执行命令
     * @return Diff结果
     */
    public DiffResult executeDiff(ExecuteDiffCommand command) {
        validateCommand(command);
        log.info("diff.execute start migrationKey={}, traceId={}, oldJsonLength={}, newJsonLength={}",
                command.migrationKey(), command.traceId(),
                command.oldJson() == null ? null : command.oldJson().length(),
                command.newJson() == null ? null : command.newJson().length());
        DiffRequest request = new DiffRequest(
                command.migrationKey(),
                command.traceId(),
                command.oldJson(),
                command.newJson(),
                command.oldCostTimeMs(),
                command.newCostTimeMs(),
                command.grayscaleParam(),
                command.oldSuccess(),
                command.newSuccess(),
                command.oldErrorMessage(),
                command.newErrorMessage(),
                command.oldRequestParams(),
                command.newRequestParams(),
                command.migrationStatus(),
                command.grayscaleRules(),
                command.grayscaleHit(),
                command.fallbackTriggered());
        List<DiffRule> rules = diffRuleRepository.findEnabledRules(command.migrationKey());
        log.info("diff.execute rulesLoaded migrationKey={}, ruleCount={}", command.migrationKey(), rules.size());
        DiffResult result = domainService.execute(request, rules);
        diffRecordRepository.save(request, result);
        log.info("diff.execute done migrationKey={}, hasDiff={}, diffItemCount={}, costTimeMs={}",
                command.migrationKey(), result.hasDiff(), result.getDiffItems().size(), result.getCostTimeMs());
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
        if (command.oldCostTimeMs() != null && command.oldCostTimeMs() < 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "old_cost_time_ms must be greater than or equal to 0");
        }
        if (command.newCostTimeMs() != null && command.newCostTimeMs() < 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "new_cost_time_ms must be greater than or equal to 0");
        }
    }
}
