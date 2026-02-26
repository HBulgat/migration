package top.bulgat.migration.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.bulgat.migration.demo.dto.User;
import top.bulgat.migration.demo.param.DemoParamHandler;
import top.bulgat.migration.sdk.starter.annotation.Migration;

@Slf4j
@Service
public class DemoService {

    @Migration(
            key = "user-get-api",
            oldMethod = "getUserOld",
            newMethod = "getUserNew",
            fallBackMethod = "getUserFallback",
            paramHandler = DemoParamHandler.class
    )
    public User getUser(String userId) {
        log.warn("This should not be printed if migration proxy works");
        return null;
    }

    public User getUserOld(String userId) {
        log.info("Executing getUserOld for userId: {}", userId);
        return User.builder()
                .id(userId)
                .name("OldUser-" + userId)
                .age(20)
                .source("OLD_API")
                .build();
    }

    public User getUserNew(String userId) {
        log.info("Executing getUserNew for userId: {}", userId);
        return User.builder()
                .id(userId)
                .name("NewUser-" + userId) 
                .age(20)
                .source("NEW_API") 
                .build();
    }

    public User getUserFallback(String userId, Exception e) {
        log.error("Executing getUserFallback for userId: {} due to error", userId, e);
        return User.builder()
                .id(userId)
                .name("FallbackUser")
                .source("FALLBACK")
                .build();
    }
}
