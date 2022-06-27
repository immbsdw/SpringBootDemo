package cn.com.demo.zookeeper.exception;

/**
 * ZK连接异常
 */
public class ZooKeeperConnectException extends RuntimeException {

    public ZooKeeperConnectException(String message) {
        super(message);
    }

    public ZooKeeperConnectException(String message, Throwable cause) {
        super(message, cause);
    }
}
