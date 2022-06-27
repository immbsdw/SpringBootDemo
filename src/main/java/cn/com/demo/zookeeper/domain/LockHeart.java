package cn.com.demo.zookeeper.domain;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 获得锁实例的心跳
 */
@Data
@Slf4j
public class LockHeart {

    /**
     * 锁名称
     */
    private String lockName;

    /**
     * 心跳最后更新时间
     */
    private long lastTime;

    public LockHeart(String lockName) {
        this.lockName = lockName;
    }

    /**
     * dw：刷新锁的心跳：如果当前的进程是Leader（持有锁）才去刷新心跳时间，且只是刷新了本地缓存中的心跳，不会去修改zk上锁节点上的数据。
     * 更新心跳时间
     */
    public void refreshLastTime() {
        log.info("【LockHeart】心跳时间被刷新了");
        this.lastTime = System.currentTimeMillis();
    }
}
