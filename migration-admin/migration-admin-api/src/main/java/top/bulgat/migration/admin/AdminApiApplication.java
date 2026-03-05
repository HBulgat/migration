package top.bulgat.migration.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

/**
 * migration-admin-api 启动类。
 */
@MapperScan("top.bulgat.migration.admin.infrastructure.persistence.mapper")
@SpringBootApplication
public class AdminApiApplication {

    /**
     * 应用启动入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AdminApiApplication.class, args);
    }
}
