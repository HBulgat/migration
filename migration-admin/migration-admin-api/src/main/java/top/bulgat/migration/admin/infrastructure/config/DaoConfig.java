package top.bulgat.migration.admin.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.bulgat.migration.config.common.configcenter.ConfigCenterGateway;
import top.bulgat.migration.config.common.dal.AlertRuleConfigDAO;
import top.bulgat.migration.config.common.dal.AlertTemplateConfigDAO;
import top.bulgat.migration.config.common.dal.DiffRuleConfigDAO;
import top.bulgat.migration.config.common.dal.GrayRuleConfigDAO;
import top.bulgat.migration.config.common.dal.MigrationTaskConfigDAO;

@Configuration
public class DaoConfig {
    @Bean
    public DiffRuleConfigDAO diffRuleConfigDAO(ConfigCenterGateway configCenterGateway) {
        return new DiffRuleConfigDAO(configCenterGateway);
    }

    @Bean
    public GrayRuleConfigDAO grayRuleConfigDAO(ConfigCenterGateway configCenterGateway) {
        return new GrayRuleConfigDAO(configCenterGateway);
    }

    @Bean
    public MigrationTaskConfigDAO migrationTaskConfigDAO(ConfigCenterGateway configCenterGateway) {
        return new MigrationTaskConfigDAO(configCenterGateway);
    }

    @Bean
    public AlertRuleConfigDAO alertRuleConfigDAO(ConfigCenterGateway configCenterGateway) {
        return new AlertRuleConfigDAO(configCenterGateway);
    }

    @Bean
    public AlertTemplateConfigDAO alertTemplateConfigDAO(ConfigCenterGateway configCenterGateway) {
        return new AlertTemplateConfigDAO(configCenterGateway);
    }
}
