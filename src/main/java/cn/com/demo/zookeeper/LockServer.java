package cn.com.demo.zookeeper;

import cn.com.demo.comm.Common;
import cn.com.demo.zookeeper.callback.IZookeeperLockNotify;
import cn.com.demo.zookeeper.callback.impl.ZookeeperLockCallBackNotify;
import cn.com.demo.zookeeper.constant.ZKParamConstant;
import cn.com.demo.zookeeper.domain.LockHeart;
import cn.com.demo.zookeeper.domain.LockInfo;
import cn.com.demo.zookeeper.domain.LockNodeInfo;
import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

import static cn.com.demo.zookeeper.constant.ThreadPoolConstant.LOCK_THREAD_POOL;
import static org.apache.curator.framework.recipes.leader.LeaderLatch.CloseMode.NOTIFY_LEADER;
import static org.apache.curator.framework.recipes.leader.LeaderLatch.State.STARTED;

/**
 * 所有抢锁任务服务
 */
@Slf4j
public class LockServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockServer.class);

    /**
     * ZK客户端
     */
    private CuratorFramework client;

    private NodeServer nodeServer;
    private LockHeartServer lockHeartServer;
    private LockCheckServer lockCheckServer;

    /**
     * 当前ZKClient实例所有参与的抢锁，不一定是抢到锁，而是所有参与的抢锁
     * key: 锁名
     * value: 抢锁信息
     */
    @Getter
    private Map<String, LockInfo> lockInfoMap = new ConcurrentHashMap<>();

    LockServer(CuratorFramework client, NodeServer nodeServer) {
        this.client = client;
        this.nodeServer = nodeServer;
        this.lockHeartServer = new LockHeartServer(this);
        this.lockCheckServer = new LockCheckServer(this);
    }

    /**
     * 注册锁
     */
    public synchronized boolean registerLock(String lockName, IZookeeperLockNotify zookeeperLockNotify, String lockPath, String clientId) {
        // 判断重复
        if (lockInfoMap.get(lockName) != null &&
                lockInfoMap.get(lockName).getLeaderLatch().getState().equals(STARTED)) {
            LOGGER.error("注册锁【{}】失败 Message:{}", lockName, "重复注册");
            return false;
        }
        // 创建抢锁实例
        LeaderLatch leaderLatch = new LeaderLatch(this.client, lockPath, clientId, NOTIFY_LEADER);
        // 锁心跳、更新心跳时间
        LockHeart lockHeart = new LockHeart(lockName);
        log.info("【LockServer】registerLock时刷新了心跳");
        lockHeart.refreshLastTime();
        // 锁回调
        ZookeeperLockCallBackNotify lockNotify = new ZookeeperLockCallBackNotify(lockName, zookeeperLockNotify, this, this.nodeServer);
        // 本次抢锁信息
        LockInfo lockInfo = new LockInfo(lockName, leaderLatch, lockNotify, lockPath, clientId, lockHeart);
        // 添加抢锁列表
        lockInfoMap.put(lockName, lockInfo);
        try {
            leaderLatch.start();
        } catch (Exception e) {
            LOGGER.error("注册锁【{}】失败 Message:{}", lockName, Common.getStackTrace(e));
            lockInfoMap.remove(lockName);
            return false;
        }
        // 添加回调
        leaderLatch.addListener(lockNotify, LOCK_THREAD_POOL);
        LOGGER.info("注册锁【{}】成功", lockName);
        return true;
    }

    /**
     * 重新注册锁
     */
    public synchronized void registerLock(LockInfo lockInfo) {
        LOGGER.info("重新注册锁【{}】", lockInfo.getLockName());
        // 创建抢锁实例
        LeaderLatch leaderLatch = new LeaderLatch(this.client, lockInfo.getLockPath(), lockInfo.getClientId(), NOTIFY_LEADER);
        lockInfo.setLeaderLatch(leaderLatch);
        try {
            leaderLatch.start();
        } catch (Exception e) {
            LOGGER.error("重新注册锁【{}】失败 Message:{}", lockInfo.getLockName(), Common.getStackTrace(e));
            return;
        }
        // 添加回调
        leaderLatch.addListener(lockInfo.getLockNotify(), LOCK_THREAD_POOL);
    }

    /**
     * 退出抢锁(不再重新抢锁)
     */
    void quitLock(String lockName) throws IOException {
        LockInfo lockInfo = lockInfoMap.get(lockName);
        if (lockInfo != null) {
            lockInfo.setReGetLock(false);
            lockInfo.getLeaderLatch().close();
        }
    }

    /**
     * 释放锁并重新抢锁
     */
    public void releaseLock(String lockName) throws IOException {
        LockInfo lockInfo = lockInfoMap.get(lockName);
        if (lockInfo != null) {
            lockInfo.getLeaderLatch().close();
        }
    }

    /**
     * 判断是否拥有锁
     *
     */
    public boolean hasLock(String lockName) {
        LockInfo lockInfo = lockInfoMap.get(lockName);
        if (lockInfo == null) {
            LOGGER.error("判断是否拥有锁【{}】出错 没有参与抢锁", lockName);
            return false;
        }
        try {
            LeaderLatch leaderLatch = lockInfo.getLeaderLatch();
            return leaderLatch.getLeader().isLeader() && leaderLatch.hasLeadership();
        } catch (Exception e) {
            LOGGER.error("判断是否拥有锁【{}】出错 Message:{}", lockName, Common.getStackTrace(e));
        }
        return false;
    }

    void updateLockHeart(String lockName) {
        this.lockHeartServer.updateLockHeart(lockName, this);
    }

    /**
     * 关闭client
     */
    void close() throws IOException {
        this.lockCheckServer.close();
        this.lockHeartServer.close();
        for (String lockName : lockInfoMap.keySet()) {
            quitLock(lockName);
        }
        lockInfoMap.clear();
    }

    /**
     * 获取锁超时时间
     */
    long getLockTimeOut(LockInfo lockInfo) {
        try {
            LockNodeInfo lockNodeInfo = JSON.parseObject(nodeServer.getNodeData(lockInfo.getLockPath()), LockNodeInfo.class);
            return lockNodeInfo.getTimeOut();
        } catch (Exception e) {
            LOGGER.error("锁节点:{} 数据解析出错:{}", lockInfo.getLockName(), Common.getStackTrace(e));
        }
        // 解析失败使用默认超时时间
        return this.getDefaultLockTimeOut();
    }


    /**
     * 获取默认锁超时时间
     * dw:给Lock节点设置值为，心跳的超时时间，如果没设置，取默认时间180m
     */
    public long getDefaultLockTimeOut() {
        Long timeOut = null;
        try {
            String value = nodeServer.getNodeData(ZKParamConstant.LOCK_NODENAME);
            if (value != null) {
                timeOut = Long.parseLong(value);
            }
        } catch (Exception e) {
            LOGGER.error("获取{}节点，默认超时时间失败：{}", ZKParamConstant.LOCK_NODENAME, e.getMessage());
        }
        return timeOut == null || timeOut == 0 ? ZKParamConstant.HEART_TIMEOUT : timeOut;

    }

}
