package me.dingtou.options.util;

public class ExceptionUtils {

    /**
     * 抛出运行时异常
     * 
     * @param exception
     */
    public static void throwRuntimeException(Throwable exception) {
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        } else {
            throw new RuntimeException(exception);
        }
    }

}
