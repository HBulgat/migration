package top.bulgat.migration.demo.param;

import org.springframework.stereotype.Component;
import top.bulgat.migration.sdk.core.function.ParamHandler;

import java.util.HashMap;
import java.util.Map;

@Component
public class DemoParamHandler implements ParamHandler {

    @Override
    public Map<String, Object> build(Object... args) {
        Map<String, Object> params = new HashMap<>();
        if (args != null && args.length > 0) {
            params.put("userId", args[0]);
        }
        return params;
    }
}
