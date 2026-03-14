package top.bulgat.migration.admin.infrastructure.repository;

import top.bulgat.common.notice.NoticeChannel;
import top.bulgat.migration.admin.domain.model.AlertTemplate;
import top.bulgat.migration.admin.domain.repository.AlertTemplateRepository;
import top.bulgat.migration.config.common.dal.AlertTemplateConfigDAO;
import top.bulgat.migration.config.common.model.dataobject.AlertTemplateConfig;

import org.springframework.stereotype.Repository;

import java.util.Map;

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

    @Override
    public void save(AlertTemplate template) {
        Map<String, AlertTemplateConfig> templateConfigMap = alertTemplateConfigDAO.findAll();
        templateConfigMap.put(template.getTemplateKey(), this.toConfig(template));
        alertTemplateConfigDAO.save(templateConfigMap);
    }

    @Override
    public java.util.List<AlertTemplate> findAll() {
        return alertTemplateConfigDAO.findAll().entrySet().stream()
                .map(entry -> {
                    AlertTemplate template = this.toEntity(entry.getValue());
                    template.initTemplateKey(entry.getKey());
                    return template;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private AlertTemplate toEntity(AlertTemplateConfig config) {
        return new AlertTemplate(
                null, // Key will be populated separately if needed, though DAO maps this 
                NoticeChannel.fromValue(config.channel()),
                config.name(),
                config.template(),
                config.createTime(),
                config.updateTime()
        );
    }

    private AlertTemplateConfig toConfig(AlertTemplate entity) {
        return new AlertTemplateConfig(
                entity.getChannel().name(),
                entity.getName(),
                entity.getTemplate(),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }
}
