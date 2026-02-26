package top.bulgat.migration.admin.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import top.bulgat.migration.admin.domain.model.GrayscaleRule;
import top.bulgat.migration.admin.domain.model.GrayscaleRuleType;
import top.bulgat.migration.admin.infrastructure.configcenter.ConfigCenterGateway;
import top.bulgat.migration.admin.infrastructure.configcenter.InMemoryConfigCenterGateway;

class ConfigCenterGrayscaleRuleRepositoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final InMemoryConfigCenterGateway gateway = new InMemoryConfigCenterGateway();
    private final ConfigCenterGrayscaleRuleRepository repository =
            new ConfigCenterGrayscaleRuleRepository(gateway, objectMapper);

    @Test
    void saveAndFind_shouldPersistRuleWithMigrationPrefix() {
        LocalDateTime now = LocalDateTime.of(2026, 2, 23, 21, 0);
        GrayscaleRule rule = new GrayscaleRule(
                "rule-1",
                "user.query",
                GrayscaleRuleType.WHITELIST,
                "[\"u1\"]",
                true,
                now,
                now);

        repository.save(rule);

        assertTrue(gateway.getConfig("migration_user.query", "GRAYSCALE_RULE_GROUP").isPresent());
        List<GrayscaleRule> rules = repository.findByMigrationKey("user.query");
        assertEquals(1, rules.size());
        assertEquals("rule-1", rules.get(0).getRuleId());
    }

    @Test
    void save_shouldReplaceByRuleIdInsteadOfDuplicating() {
        GrayscaleRule origin = new GrayscaleRule(
                "rule-1",
                "user.query",
                GrayscaleRuleType.WHITELIST,
                "[\"u1\"]",
                true);
        GrayscaleRule updated = new GrayscaleRule(
                "rule-1",
                "user.query",
                GrayscaleRuleType.BLACKLIST,
                "[\"u2\"]",
                false);

        repository.save(origin);
        repository.save(updated);

        List<GrayscaleRule> rules = repository.findByMigrationKey("user.query");
        assertEquals(1, rules.size());
        assertEquals(GrayscaleRuleType.BLACKLIST, rules.get(0).getRuleType());
        assertFalse(rules.get(0).isEnable());
    }

    @Test
    void delete_shouldSupportDeleteByRuleIdAndMigrationKey() {
        GrayscaleRule rule = new GrayscaleRule(
                "rule-1",
                "user.query",
                GrayscaleRuleType.WHITELIST,
                "[\"u1\"]",
                true);
        repository.save(rule);

        repository.deleteByMigrationKeyAndRuleId("user.query", "rule-1");
        assertTrue(repository.findByMigrationKey("user.query").isEmpty());

        repository.save(rule);
        repository.deleteByMigrationKey("user.query");
        assertFalse(gateway.getConfig("migration_user.query", "GRAYSCALE_RULE_GROUP").isPresent());
    }





    @Test
    void findByMigrationKey_shouldFallbackToMigrationPrefixInDefaultGroup() {
        gateway.publish("migration_user.query",
                "[\n" +
                        "  {\"rule_id\":\"rule-default\",\"migration_key\":\"user.query\",\"rule_type\":\"WHITELIST\",\"rule_value\":\"[\\\"u1\\\"]\",\"enable\":true}\n" +
                        "]");

        List<GrayscaleRule> rules = repository.findByMigrationKey("user.query");

        assertEquals(1, rules.size());
        assertEquals("rule-default", rules.get(0).getRuleId());
    }

    @Test
    void findByMigrationKey_shouldFallbackToLegacyGrayscaleKey() {
        gateway.publish("grayscale_user.query",
                "[\n" +
                        "  {\"rule_id\":\"rule-legacy\",\"migration_key\":\"user.query\",\"rule_type\":\"WHITELIST\",\"rule_value\":\"[\\\"u1\\\"]\",\"enable\":true}\n" +
                        "]");

        List<GrayscaleRule> rules = repository.findByMigrationKey("user.query");

        assertEquals(1, rules.size());
        assertEquals("rule-legacy", rules.get(0).getRuleId());
    }

    @Test
    void findByMigrationKey_shouldThrowWhenRulePayloadMalformed() {
        gateway.publish("migration_user.query", "GRAYSCALE_RULE_GROUP", "{");

        assertThrows(IllegalStateException.class, () -> repository.findByMigrationKey("user.query"));
    }

    @Test
    void save_shouldWrapPublishException() {
        ConfigCenterGateway brokenGateway = Mockito.mock(ConfigCenterGateway.class);
        when(brokenGateway.getConfig("migration_user.query", "GRAYSCALE_RULE_GROUP")).thenReturn(java.util.Optional.empty());
        when(brokenGateway.getConfig("migration_user.query", "DEFAULT_GROUP")).thenReturn(java.util.Optional.empty());
        when(brokenGateway.getConfig("grayscale_user.query", "DEFAULT_GROUP")).thenReturn(java.util.Optional.empty());
        doThrow(new RuntimeException("nacos timeout"))
                .when(brokenGateway)
                .publish(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        ConfigCenterGrayscaleRuleRepository brokenRepository =
                new ConfigCenterGrayscaleRuleRepository(brokenGateway, objectMapper);

        GrayscaleRule rule = new GrayscaleRule(
                "rule-1",
                "user.query",
                GrayscaleRuleType.WHITELIST,
                "[\"u1\"]",
                true);

        assertThrows(IllegalStateException.class, () -> brokenRepository.save(rule));
    }
}
