package top.bulgat.migration.admin.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import top.bulgat.migration.admin.domain.model.MigrationStatus;
import top.bulgat.migration.admin.domain.model.MigrationTask;
import top.bulgat.migration.admin.infrastructure.configcenter.ConfigCenterGateway;
import top.bulgat.migration.admin.infrastructure.configcenter.InMemoryConfigCenterGateway;

class ConfigCenterMigrationTaskRepositoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final InMemoryConfigCenterGateway gateway = new InMemoryConfigCenterGateway();
    private final ConfigCenterMigrationTaskRepository repository =
            new ConfigCenterMigrationTaskRepository(gateway, objectMapper);

    @Test
    void saveAndFind_shouldPersistTaskToConfigCenter() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 2, 23, 21, 0);
        MigrationTask task = new MigrationTask("user.query", MigrationStatus.OLD, "desc", now, now);

        repository.save(task);

        assertTrue(repository.existsByMigrationKey("user.query"));
        assertTrue(gateway.getConfig("migration_user.query", "MIGRATION_TASK_GROUP").isPresent());
        assertEquals(1, repository.findAll().size());

        List<String> index = objectMapper.readValue(
                gateway.getConfig("migration_index", "MIGRATION_TASK_GROUP").orElseThrow(),
                new TypeReference<List<String>>() {
                });
        assertEquals(List.of("user.query"), index);
    }

    @Test
    void deleteByMigrationKey_shouldRemoveTaskAndIndex() {
        MigrationTask first = new MigrationTask("user.query", MigrationStatus.OLD, "desc");
        MigrationTask second = new MigrationTask("order.sync", MigrationStatus.OLD, "desc");
        repository.save(first);
        repository.save(second);

        repository.deleteByMigrationKey("user.query");

        assertFalse(repository.existsByMigrationKey("user.query"));
        assertTrue(repository.existsByMigrationKey("order.sync"));
        assertEquals(1, repository.findAll().size());
    }



    @Test
    void findByMigrationKey_shouldFallbackToDefaultGroup() {
        gateway.publish("migration_user.query", "{\"migration_key\":\"user.query\",\"status\":1,\"description\":\"desc\"}");

        assertTrue(repository.findByMigrationKey("user.query").isPresent());
    }

    @Test
    void findByMigrationKey_shouldThrowWhenTaskPayloadMalformed() {
        gateway.publish("migration_user.query", "MIGRATION_TASK_GROUP", "{");

        assertThrows(IllegalStateException.class, () -> repository.findByMigrationKey("user.query"));
    }

    @Test
    void save_shouldWrapPublishException() {
        ConfigCenterGateway brokenGateway = Mockito.mock(ConfigCenterGateway.class);
        when(brokenGateway.getConfig("migration_index", "MIGRATION_TASK_GROUP")).thenReturn(java.util.Optional.empty());
        when(brokenGateway.getConfig("migration_index", "DEFAULT_GROUP")).thenReturn(java.util.Optional.empty());
        doThrow(new RuntimeException("nacos timeout"))
                .when(brokenGateway)
                .publish(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        ConfigCenterMigrationTaskRepository brokenRepository =
                new ConfigCenterMigrationTaskRepository(brokenGateway, objectMapper);

        MigrationTask task = new MigrationTask("user.query", MigrationStatus.OLD, "desc");

        assertThrows(IllegalStateException.class, () -> brokenRepository.save(task));
    }
}
