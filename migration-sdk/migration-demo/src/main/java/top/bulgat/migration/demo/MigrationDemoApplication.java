package top.bulgat.migration.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import top.bulgat.migration.sdk.starter.annotation.EnableMigration;

@EnableMigration
@SpringBootApplication
public class MigrationDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MigrationDemoApplication.class, args);
    }
}
