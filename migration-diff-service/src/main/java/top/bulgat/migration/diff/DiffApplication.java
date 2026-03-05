package top.bulgat.migration.diff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

/**
 * migration-diff 启动类。
 */
@MapperScan("top.bulgat.migration.diff.infrastructure.persistence.mapper")
@SpringBootApplication
public class DiffApplication {

    /**
     * 应用启动入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DiffApplication.class, args);
    }
}
