package top.bulgat.migration.diff.infrastructure.repository.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import top.bulgat.common.notice.NoticeChannel;
import top.bulgat.migration.config.common.dal.AlertRuleConfigDAO;
import top.bulgat.migration.config.common.dal.AlertTemplateConfigDAO;
import top.bulgat.migration.config.common.model.dataobject.AlertTemplateConfig;
import top.bulgat.migration.diff.domain.model.AlertRule;
import top.bulgat.migration.diff.domain.model.AlertTemplate;
import top.bulgat.migration.diff.domain.repository.AlertTemplateRepository;

import java.util.Map;

/**
 * DefaultAlertTemplateRepository 定义持久化访问能力。
 */
@Repository
public class DefaultAlertTemplateRepository implements AlertTemplateRepository {

    private final AlertTemplateConfigDAO alertTemplateConfigDAO;

    public DefaultAlertTemplateRepository(AlertTemplateConfigDAO alertTemplateConfigDAO){
        this.alertTemplateConfigDAO=alertTemplateConfigDAO;
    }

    @Override
    public AlertTemplate findByTemplateKey(String templateKey) {
        Map<String, AlertTemplateConfig> templateConfigMap = alertTemplateConfigDAO.findAll();
        if (templateConfigMap.containsKey(templateKey)){
            return this.toEntity(templateConfigMap.get(templateKey));
        }
        return null;
    }

    private AlertTemplate toEntity(AlertTemplateConfig config) {
        return new AlertTemplate(
                NoticeChannel.fromValue(config.channel()),
                config.name(),
                config.template()
        );
    }

}
