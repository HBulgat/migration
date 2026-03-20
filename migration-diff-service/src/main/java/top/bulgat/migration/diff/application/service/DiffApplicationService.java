package top.bulgat.migration.diff.application.service;

import java.util.List;
import java.util.stream.Collectors;
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
import top.bulgat.migration.diff.domain.service.AlertService;
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
    private final AlertService alertService;

    public DiffApplicationService(
            DiffDomainService domainService,
            DiffRecordRepository diffRecordRepository,
            DiffRuleRepository diffRuleRepository,
            AlertService alertService) {
        this.domainService = domainService;
        this.diffRecordRepository = diffRecordRepository;
        this.diffRuleRepository = diffRuleRepository;
        this.alertService = alertService;
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
        DiffRequest request = command.toDiffRequest();
        List<DiffRule> rules = diffRuleRepository.findEnabledRules(command.migrationKey());
        log.info("diff.execute rulesLoaded migrationKey={}, ruleCount={}", command.migrationKey(), rules.size());
        DiffResult result = domainService.execute(request, rules);
        diffRecordRepository.save(request, result);
        log.info("request={}, result={}",request,result);
        alertService.alertIfNeeded(request, result);
        log.info("diff.execute done migrationKey={}, hasDiff={}, diffItemCount={}, costTimeMs={}",
                command.migrationKey(), result.hasDiff(), result.getDiffItems().size(), result.getCostTimeMs());
        return result;
    }

    /**
     * 批量执行Diff比对用例，最大化吞吐量。
     *
     * @param commands Diff 批量执行命令
     * @return 批量Diff结果
     */
    public List<DiffResult> executeDiffBatch(List<ExecuteDiffCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return List.of();
        }

        long start = System.currentTimeMillis();
        // 核心优化：Diff比对是CPU密集型操作（JSON解析与对比），使用 parallelStream 最大化吞吐量
        List<DiffRequestResultPair> pairs = commands.parallelStream()
                .map(command -> {
                    validateCommand(command);
                    DiffRequest request = command.toDiffRequest();
                    // 规则通常带有本地缓存，按 key 查询极快
                    List<DiffRule> rules = diffRuleRepository.findEnabledRules(command.migrationKey());
                    DiffResult result = domainService.execute(request, rules);
                    return new DiffRequestResultPair(request, result);
                })
                .collect(Collectors.toList());

        // 批量落库，减少数据库交互频率带来的IO等待
        diffRecordRepository.saveBatch(
                pairs.stream().map(DiffRequestResultPair::request).collect(Collectors.toList()),
                pairs.stream().map(DiffRequestResultPair::result).collect(Collectors.toList())
        );

        // 告警处理（保持异步特性，不阻塞当前线程池）
        for (DiffRequestResultPair pair : pairs) {
            alertService.alertIfNeeded(pair.request(), pair.result());
        }

        log.info("diff.executeBatch done batchSize={}, costTimeMs={}", commands.size(), System.currentTimeMillis() - start);

        return pairs.stream().map(DiffRequestResultPair::result).collect(Collectors.toList());
    }

    // 内部结构体用于流处理中的结果透传
    private record DiffRequestResultPair(DiffRequest request, DiffResult result) {}

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
