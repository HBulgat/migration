package top.bulgat.migration.admin.infrastructure.repository;

import top.bulgat.common.notice.NoticeChannel;
import top.bulgat.migration.admin.domain.model.AlertTemplate;
import top.bulgat.migration.admin.domain.repository.AlertTemplateRepository;
import top.bulgat.migration.config.common.dal.AlertTemplateConfigDAO;
import top.bulgat.migration.config.common.model.dataobject.AlertTemplateConfig;

import java.util.Map;

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
