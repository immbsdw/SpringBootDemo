package cn.com.demo.zookeeper.callback;

/**
 * 获取锁回调
 */
public interface IZookeeperLockNotify {

    /**
     * 获得锁
     */
    void onEntryLock(String lockName);

    /**
     * 失去锁
     */
    void onExitLock(String lockName);
}
