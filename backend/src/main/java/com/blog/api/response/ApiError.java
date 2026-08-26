package com.blog.api.response;

import java.util.Map;

public final class ApiError {

    private final String code;
    private final String message;
    private final Map<String, String> fieldErrors;

    public ApiError(String code, String message, Map<String, String> fieldErrors) {
        this.code = code;
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
