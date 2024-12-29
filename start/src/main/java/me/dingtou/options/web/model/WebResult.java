package me.dingtou.options.web.model;

import lombok.Data;

@Data
public class WebResult<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> WebResult<T> success(T data) {
        WebResult<T> result = new WebResult<>();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }
    public static <T> WebResult<T> failure(String message) {
        WebResult<T> result = new WebResult<>();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
}
