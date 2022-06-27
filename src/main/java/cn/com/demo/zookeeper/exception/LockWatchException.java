package cn.com.demo.zookeeper.exception;

/**
 * 锁异常
 */
public class LockWatchException extends RuntimeException {

    public LockWatchException(String message) {
        super(message);
    }

    public LockWatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
