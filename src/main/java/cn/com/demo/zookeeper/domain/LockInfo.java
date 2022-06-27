package cn.com.demo.zookeeper.domain;

import cn.com.demo.zookeeper.callback.impl.ZookeeperLockCallBackNotify;
import lombok.Data;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

/**
 * dw：锁信息，放在本地缓存中
 */
@Data
public class LockInfo {

    /**
     * 抢锁实例
     */
    private LeaderLatch leaderLatch;

    /**
     * 锁名称
     */
    private String lockName;

    /**
     * 锁节点地址
     */
    private String lockPath;

    /**
     * zkClient标示
     */
    private String clientId;

    /**
     * 失去锁后是否重新抢锁(默认是)
     */
    private boolean reGetLock;

    /**
     * 锁心跳
     */
    private LockHeart lockHeart;

    /**
     * 锁通知
     */
    private ZookeeperLockCallBackNotify lockNotify;

    /**
     * 是否获取到锁(是否通知过)
     */
    private boolean lockLeader;

    public LockInfo(String lockName, LeaderLatch leaderLatch, ZookeeperLockCallBackNotify lockNotify, String lockPath, String clientId, LockHeart lockHeart) {
        this.leaderLatch = leaderLatch;
        this.lockNotify = lockNotify;
        this.lockPath = lockPath;
        this.clientId = clientId;
        //dw：原为true
        this.reGetLock = false;
        this.lockHeart = lockHeart;
        this.lockName = lockName;
    }
}
