package cn.com.demo.zookeeper.exception;

/**
 * ZK客户端身份识别异常
 */
public class ZooKeeperClientIdentityException extends RuntimeException {

    public ZooKeeperClientIdentityException(String message) {
        super(message);
    }

    public ZooKeeperClientIdentityException(String message, Throwable cause) {
        super(message, cause);
    }
}
