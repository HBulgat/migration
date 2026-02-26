package top.bulgat.migration.admin.interfaces.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import top.bulgat.common.exception.BizException;
import top.bulgat.common.exception.ErrorCode;
import top.bulgat.common.model.Result;
import top.bulgat.common.springboot.middleware.exception.GlobalExceptionHandler;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBizException_shouldReturnBusinessCodeAndMessage() {
        BizException ex = new BizException(ErrorCode.NOT_FOUND, "task not found");

        Result<Void> result = handler.handleBizException(ex);

        assertEquals(ErrorCode.NOT_FOUND.getCode(), result.getCode());
        assertEquals("task not found", result.getMessage());
    }

    @Test
    void handleValidation_shouldReturnBadRequestWithJoinedMessages() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "migrationKey", "must not be blank"));
        bindingResult.addError(new FieldError("request", "status", "must be between 1 and 7"));
        Method method = DummyValidationTarget.class.getDeclaredMethod("validate", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        Result<Void> result = handler.handleValidation(ex);

        assertEquals(400, result.getCode());
        assertTrue(result.getMessage().contains("must not be blank"));
        assertTrue(result.getMessage().contains("must be between 1 and 7"));
    }

    @Test
    void handleBind_shouldReturnBadRequestCode() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "query");
        bindingResult.addError(new FieldError("query", "page", "must be greater than or equal to 1"));
        BindException ex = new BindException(bindingResult);

        Result<Void> result = handler.handleBind(ex);

        assertEquals(400, result.getCode());
        assertEquals("must be greater than or equal to 1", result.getMessage());
    }

    @Test
    void handleAll_shouldHideInternalDetails() {
        RuntimeException ex = new RuntimeException("db timeout");

        Result<Void> result = handler.handleAll(ex);

        assertEquals(500, result.getCode());
        assertEquals("系统繁忙，请稍后重试", result.getMessage());
    }

    private static final class DummyValidationTarget {
        @SuppressWarnings("unused")
        void validate(String value) {
        }
    }
}
