package top.bulgat.migration.admin.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.bulgat.common.base.exception.BizException;
import top.bulgat.common.notice.NoticeChannel;
import top.bulgat.migration.admin.application.command.*;
import top.bulgat.migration.admin.domain.model.AlertRule;
import top.bulgat.migration.admin.domain.repository.AlertRuleRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AlertRuleApplicationService {

    private final AlertRuleRepository alertRuleRepository;

    public AlertRuleApplicationService(AlertRuleRepository alertRuleRepository) {
        this.alertRuleRepository = alertRuleRepository;
    }

    public String createAlertRule(CreateAlertRuleCommand cmd) {
        // 生成唯一标识
        String ruleId = UUID.randomUUID().toString();
        // 验证渠道正确性
        NoticeChannel channel = validateChannel(cmd.getChannel());

        AlertRule rule = new AlertRule(
                cmd.getMigrationKey(),
                ruleId,
                cmd.getName(),
                cmd.getEnable(),
                channel,
                cmd.getTemplateKey(),
                cmd.getReceivers(),
                LocalDateTime.now(),
                LocalDateTime.now());

        alertRuleRepository.save(rule);
        return ruleId;
    }

    public void updateAlertRule(UpdateAlertRuleCommand cmd) {
        List<AlertRule> rules = alertRuleRepository.findByMigrationKey(cmd.getMigrationKey());
        AlertRule rule = rules.stream().filter(r -> r.getRuleId().equals(cmd.getRuleId())).findFirst()
                .orElseThrow(() -> new BizException("AlertRule not found: " + cmd.getRuleId()));

        NoticeChannel channel = cmd.getChannel() != null ? validateChannel(cmd.getChannel()) : null;

        rule.update(cmd.getName(), channel, cmd.getTemplateKey(), cmd.getReceivers(), cmd.getEnable());
        alertRuleRepository.save(rule);
    }

    public void updateAlertRuleEnable(UpdateAlertRuleEnableCommand cmd) {
        List<AlertRule> rules = alertRuleRepository.findByMigrationKey(cmd.getMigrationKey());
        AlertRule rule = rules.stream().filter(r -> r.getRuleId().equals(cmd.getRuleId())).findFirst()
                .orElseThrow(() -> new BizException("AlertRule not found: " + cmd.getRuleId()));

        rule.changeEnable(cmd.getEnable());
        alertRuleRepository.save(rule);
    }

    public void deleteAlertRule(DeleteAlertRuleCommand cmd) {
        alertRuleRepository.deleteByRuleId(cmd.getMigrationKey(), cmd.getRuleId());
    }

    public List<AlertRule> listAlertRules(ListAlertRuleCommand cmd) {
        List<AlertRule> rules = alertRuleRepository.findByMigrationKey(cmd.getMigrationKey());
        if (cmd.getChannel() != null && !cmd.getChannel().isEmpty()) {
            return rules.stream()
                    .filter(r -> r.getChannel().name().equalsIgnoreCase(cmd.getChannel()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return rules;
    }

    private NoticeChannel validateChannel(String channelStr) {
        try {
            return NoticeChannel.fromValue(channelStr);
        } catch (IllegalArgumentException e) {
            throw new BizException("Unsupported notice channel: " + channelStr);
        }
    }
}
