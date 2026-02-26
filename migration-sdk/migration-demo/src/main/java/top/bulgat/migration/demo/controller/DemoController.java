package top.bulgat.migration.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.bulgat.common.model.Result;
import top.bulgat.migration.demo.dto.User;
import top.bulgat.migration.demo.service.DemoService;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @GetMapping("/user/{id}")
    public Result<User> getUser(@PathVariable String id) {
        User user = demoService.getUser(id);
        return Result.success(user);
    }
}
