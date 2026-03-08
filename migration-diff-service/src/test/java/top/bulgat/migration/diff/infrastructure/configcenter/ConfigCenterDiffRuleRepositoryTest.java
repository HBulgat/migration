//package top.bulgat.migration.diff.infrastructure.configcenter;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import com.alibaba.nacos.api.config.ConfigService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import java.util.List;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import top.bulgat.migration.config.common.configcenter.NacosConfigCenterGateway;
//import top.bulgat.migration.config.common.dal.DiffRuleConfigDAO;
//import top.bulgat.migration.diff.domain.model.DiffRule;
//import top.bulgat.migration.diff.domain.model.DiffRuleType;
//import top.bulgat.migration.diff.infrastructure.repository.config.DefaultDiffRuleRepository;
//
//class DefaultDiffRuleRepositoryTest {
//
//  private final ObjectMapper objectMapper = new ObjectMapper();
//  private final ConfigService configService = Mockito.mock(ConfigService.class);
//  private final DefaultDiffRuleRepository repository = new DefaultDiffRuleRepository(new DiffRuleConfigDAO(new ()));
//
//  @Test
//    void findEnabledRules_shouldFilterDisabledRules() throws Exception {
//        when(configService.getConfig("migration_user.query", "DIFF_RULE_GROUP", 3000)).thenReturn("""
//                [
//                  {
//                    "migration_key": "user.query",
//                    "rule_type": "IGNORE",
//                    "field_path": "$.name",
//                    "rule_value": "",
//                    "enable": true
//                  },
//                  {
//                    "migration_key": "user.query",
//                    "rule_type": "SORT",
//                    "field_path": "$.items",
//                    "rule_value": "id",
//                    "enable": false
//                  }
//                ]
//                """);
//
//        List<DiffRule> rules = repository.findEnabledRules("user.query");
//
//        assertEquals(1, rules.size());
//        assertEquals(DiffRuleType.IGNORE, rules.get(0).ruleType());
//        assertEquals("$.name", rules.get(0).fieldPath());
//        verify(configService).getConfig("migration_user.query", "DIFF_RULE_GROUP", 3000);
//    }
//
//  @Test
//    void findEnabledRules_shouldReturnEmptyWhenConfigBlank() throws Exception {
//        when(configService.getConfig("migration_user.query", "DIFF_RULE_GROUP", 3000)).thenReturn("  ");
//
//        List<DiffRule> rules = repository.findEnabledRules("user.query");
//
//        assertTrue(rules.isEmpty());
//    }
//
//  @Test
//    void findEnabledRules_shouldIgnoreInvalidRuleType() throws Exception {
//        when(configService.getConfig("migration_user.query", "DIFF_RULE_GROUP", 3000)).thenReturn("""
//                [
//                  {
//                    "migration_key": "user.query",
//                    "rule_type": "UNKNOWN",
//                    "field_path": "$.name",
//                    "rule_value": "",
//                    "enable": true
//                  }
//                ]
//                """);
//
//        List<DiffRule> rules = repository.findEnabledRules("user.query");
//
//        assertTrue(rules.isEmpty());
//    }
//
//  @Test
//    void findEnabledRules_shouldReturnEmptyWhenConfigMalformed() throws Exception {
//        when(configService.getConfig("migration_user.query", "DIFF_RULE_GROUP", 3000)).thenReturn("{");
//
//        List<DiffRule> rules = repository.findEnabledRules("user.query");
//
//        assertTrue(rules.isEmpty());
//    }
//
//  @Test
//    void findEnabledRules_shouldReturnEmptyWhenNacosTimeout() throws Exception {
//        when(configService.getConfig("migration_user.query", "DIFF_RULE_GROUP", 3000))
//                .thenThrow(new RuntimeException("timeout"));
//
//        List<DiffRule> rules = repository.findEnabledRules("user.query");
//
//        assertTrue(rules.isEmpty());
//    }
//
//  @Test
//    void findEnabledRules_shouldFallbackMigrationKeyWhenRuleMigrationKeyBlank() throws Exception {
//        when(configService.getConfig("migration_user.query", "DIFF_RULE_GROUP", 3000)).thenReturn("""
//                [
//                  {
//                    "migration_key": "",
//                    "rule_type": "IGNORE",
//                    "field_path": "$.name",
//                    "rule_value": "",
//                    "enable": true
//                  }
//                ]
//                """);
//
//        List<DiffRule> rules = repository.findEnabledRules("user.query");
//
//        assertEquals(1, rules.size());
//        assertEquals("user.query", rules.get(0).migrationKey());
//    }
//
//  @Test
//    void findEnabledRules_shouldKeepValidRulesWhenConfigContainsInvalidItems() throws Exception {
//        when(configService.getConfig("migration_user.query", "DIFF_RULE_GROUP", 3000)).thenReturn("""
//                [
//                  {
//                    "migration_key": "user.query",
//                    "rule_type": "IGNORE",
//                    "field_path": "$.name",
//                    "rule_value": "",
//                    "enable": true
//                  },
//                  {
//                    "migration_key": "user.query",
//                    "rule_type": "UNKNOWN",
//                    "field_path": "$.age",
//                    "rule_value": "",
//                    "enable": true
//                  },
//                  null,
//                  {
//                    "migration_key": "",
//                    "rule_type": "SCRIPT",
//                    "field_path": "$.score",
//                    "rule_value": "#newValue == #oldValue",
//                    "enable": true
//                  }
//                ]
//                """);
//
//        List<DiffRule> rules = repository.findEnabledRules("user.query");
//
//        assertEquals(2, rules.size());
//        assertEquals(DiffRuleType.IGNORE, rules.get(0).ruleType());
//        assertEquals("user.query", rules.get(1).migrationKey());
//        assertEquals(DiffRuleType.SCRIPT, rules.get(1).ruleType());
//    }
//
//  @Test
//    void findEnabledRules_shouldFallbackToMigrationKeyInDefaultGroup() throws Exception {
//        when(configService.getConfig("migration_user.query", "DIFF_RULE_GROUP", 3000)).thenReturn(" ");
//        when(configService.getConfig("migration_user.query", "DEFAULT_GROUP", 3000)).thenReturn("""
//                [
//                  {
//                    "migration_key": "user.query",
//                    "rule_type": "IGNORE",
//                    "field_path": "$.name",
//                    "rule_value": "",
//                    "enable": true
//                  }
//                ]
//                """);
//
//        List<DiffRule> rules = repository.findEnabledRules("user.query");
//
//        assertEquals(1, rules.size());
//        assertEquals(DiffRuleType.IGNORE, rules.get(0).ruleType());
//    }
//
//  @Test
//    void findEnabledRules_shouldFallbackToLegacyDiffKeyWhenNewKeyBlank() throws Exception {
//        when(configService.getConfig("migration_user.query", "DIFF_RULE_GROUP", 3000)).thenReturn(" ");
//        when(configService.getConfig("diff_user.query", "DEFAULT_GROUP", 3000)).thenReturn("""
//                [
//                  {
//                    "migration_key": "user.query",
//                    "rule_type": "IGNORE",
//                    "field_path": "$.name",
//                    "rule_value": "",
//                    "enable": true
//                  }
//                ]
//                """);
//
//        List<DiffRule> rules = repository.findEnabledRules("user.query");
//
//        assertEquals(1, rules.size());
//        assertEquals(DiffRuleType.IGNORE, rules.get(0).ruleType());
//    }
//
//  @Test
//    void findEnabledRules_shouldReturnEmptyWhenConfigIsJsonNull() throws Exception {
//        when(configService.getConfig("migration_user.query", "DIFF_RULE_GROUP", 3000)).thenReturn("null");
//
//        List<DiffRule> rules = repository.findEnabledRules("user.query");
//
//        assertTrue(rules.isEmpty());
//    }
//
//}
