package top.bulgat.migration.diff.infrastructure.config;

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
public class DAOConfig {

    @Bean
    public DiffRuleConfigDAO diffRuleConfigDAO(ConfigCenterGateway configCenterGateway, ObjectMapper objectMapper) {
        return new DiffRuleConfigDAO(configCenterGateway, objectMapper);
    }

    @Bean
    public GrayRuleConfigDAO grayRuleConfigDAO(ConfigCenterGateway configCenterGateway, ObjectMapper objectMapper) {
        return new GrayRuleConfigDAO(configCenterGateway, objectMapper);
    }

    @Bean
    public MigrationTaskConfigDAO migrationTaskConfigDAO(ConfigCenterGateway configCenterGateway,
                                                         ObjectMapper objectMapper) {
        return new MigrationTaskConfigDAO(configCenterGateway, objectMapper);
    }

    @Bean
    public AlertRuleConfigDAO alertRuleConfigDAO(ConfigCenterGateway configCenterGateway, ObjectMapper objectMapper) {
        return new AlertRuleConfigDAO(configCenterGateway, objectMapper);
    }

    @Bean
    public AlertTemplateConfigDAO alertTemplateConfigDAO(ConfigCenterGateway configCenterGateway,
                                                         ObjectMapper objectMapper) {
        return new AlertTemplateConfigDAO(configCenterGateway, objectMapper);
    }
}
