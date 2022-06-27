package cn.com.demo.zookeeper.exception;

/**
 * 节点监控异常
 */
public class ConfigWatchException extends RuntimeException {

    public ConfigWatchException(String message) {
        super(message);
    }

    public ConfigWatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
