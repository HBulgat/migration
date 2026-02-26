package top.bulgat.migration.sdk.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import top.bulgat.migration.sdk.starter.config.MigrationAutoConfiguration;

/**
 * 启用迁移能力。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MigrationAutoConfiguration.class)
public @interface EnableMigration {
}
