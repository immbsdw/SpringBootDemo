package cn.com.demo.zookeeper;
import cn.com.demo.comm.Common;
import cn.com.demo.zookeeper.constant.ZKParamConstant;
import cn.com.demo.zookeeper.domain.LockInfo;
import lombok.extern.slf4j.Slf4j;


import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static cn.com.demo.zookeeper.constant.ThreadPoolConstant.CHECK_THREAD_POOL;
import static org.apache.curator.framework.recipes.leader.LeaderLatch.State.CLOSED;

/**
 * 定时检测每个处理机的锁状态
 */
@Slf4j
class LockCheckServer {

    private ScheduledExecutorService lockCheckScheduledThreadPool;

    public LockCheckServer(LockServer lockServer) {
        // 创建锁检测定时器
        this.lockCheckScheduledThreadPool = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("锁检测线程池");
                return thread;
            }
        });
        this.start(lockCheckScheduledThreadPool, lockServer);
    }

    /**
     * 开启检测
     */
    void start(ScheduledExecutorService checkThreadPool, LockServer lockServer) {
        // 主机检测定时锁
        checkThreadPool.scheduleWithFixedDelay(() -> {
            try {
                log.info("【LockCheckServer】锁检查定时任务运行");
                check(lockServer);
            } catch (Exception e) {
                log.error("检测锁处理失败 Message:{}", Common.getStackTrace(e));
            }
            // 检测时间小于通知锁的等待时间
        }, 0, ZKParamConstant.LOCK_CHECKTIME, TimeUnit.SECONDS);
    }

    /**
     * 检测锁状态
     * 兜底方案：保证当前实例的状态是正确的
     * dw:保持最终一致性，本地缓存中的信息及本实例的类型与zk中信息的最终一致性
     * dw：比如：当zk中关于锁的信息发生了改变，但是由于瞬时的网络的波动导致当前实例没有接收到数据，那么可能导致当前实例的状态是异常的
     * dw:因此需要通过该方法来定时的扫描当前实例的状态是否异常，并针对返回的结果进行对当前实例状态的修复
     * 异常状态包括：
     * （1）
     * （2）当前实例不是Leader，但其实zk中数据已经修改，当前实例未收到设置未Leader的指令，运行isLeader逻辑
     * （3）当前实例是Leader，但其实zk中数据已经修改，当前实例未收到不是Leader指令，运行notLeader逻辑
     */
    private void check(LockServer lockServer) {
        for (Map.Entry<String, LockInfo> entry : lockServer.getLockInfoMap().entrySet()) {
            String lockName = entry.getKey();
            LockInfo lockInfo = entry.getValue();
            log.info("【LockCheckServer】提交任务到CHECK_THREAD_POOL");
            CHECK_THREAD_POOL.submit(new Runnable() {
                @Override
                public void run() {
                    log.info("【LockCheckServer】CHECK_THREAD_POOL中：任务开始执行，检测锁"+lockName);
                    // 检测重新获取锁
                    if (lockInfo.getLeaderLatch().getState().equals(CLOSED) && lockInfo.isReGetLock()) {
                        log.info("【LockCheckServer】CHECK_THREAD_POOL中：LeaderLatch is closed,重新选举leader");
                        lockServer.registerLock(lockInfo);
                        return;
                    }
                    // 检测获得锁
                    if (!lockInfo.isLockLeader() && lockServer.hasLock(lockName)) {
                        log.info("【LockCheckServer】CHECK_THREAD_POOL中："+lockName+"原来不是leader，重新选举后成了leader，主动调用isLeader()");
                        lockInfo.getLockNotify().isLeader(CHECK_THREAD_POOL);
                        return;
                    }
                    // 检测失去锁
                    if (lockInfo.isLockLeader() && !lockServer.hasLock(lockName)) {
                        log.info("【LockCheckServer】CHECK_THREAD_POOL中："+lockName+"原来是leader，重新选举后成了普通实例，主动调用notLeader()");
                        lockInfo.getLockNotify().notLeader(CHECK_THREAD_POOL);
                    }
                }
            });
        }
    }

    /**
     * 关闭ZKClient
     */
    void close() {
        if (lockCheckScheduledThreadPool != null) {
            lockCheckScheduledThreadPool.shutdown();
        }
    }

}
