package cn.com.demo.zookeeper;

import cn.com.demo.comm.Common;
import cn.com.demo.zookeeper.constant.ZKParamConstant;
import cn.com.demo.zookeeper.domain.LockHeart;
import cn.com.demo.zookeeper.domain.LockInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static cn.com.demo.zookeeper.constant.ThreadPoolConstant.HEART_THREAD_POOL;


/**
 * 所有锁心跳检测线程(删除调度中心心跳监控。该监控无法检测出调度中心是否正常工作。)
 */
@Slf4j
class LockHeartServer {

    private ScheduledExecutorService lockHeartScheduledThreadPool;

    /**
     * 刷新心跳日志记录
     */
    private Map<String, List<RefreshLockHeartLog>> refreshLockHeartLogMap = new HashMap<>();

    public LockHeartServer(LockServer lockServer) {
        // 创建锁心跳线程池
        this.lockHeartScheduledThreadPool = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("锁心跳线程池");
                return thread;
            }
        });
        // 删除调度中心心跳监控。该监控无法检测出调度中心是否正常工作。
         this.start(lockHeartScheduledThreadPool, lockServer);
    }

    /**
     * 开启检测
     */
    void start(ScheduledExecutorService heartThreadPool, LockServer lockServer) {
        // 心跳检测
        heartThreadPool.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    check(lockServer);
                } catch (Exception e) {
                    log.error("锁心跳超时处理失败 Message:{}", Common.getStackTrace(e));

                }
            }
        }, 0, ZKParamConstant.HEART_CHECKTIME, TimeUnit.SECONDS);
    }

    /**
     * 检测心跳时间
     * 通过LockInfo缓存来判断锁是否超时，锁超时Leader会去主动释放锁，表示Leader中有业务逻辑发生了阻塞，需要主动的释放锁，
     * 让其他线程获取到锁来成为Leader。
     * 即：普通实例无法去检测Leader到的超时，抢夺锁成为Leader，而是Leader自己检测到阻塞，去主动的释放锁，来让其他线程成为Leader。
     */
    private void check(LockServer lockServer) {
        log.info("【LockHeartServer】开始检测是否存在锁超时");
        for (Map.Entry<String, LockInfo> entry : lockServer.getLockInfoMap().entrySet()) {
            String lockName = entry.getKey();
            LockInfo lockInfo = entry.getValue();
            if (!lockInfo.isLockLeader()) {
                log.info("【LockHeartServer】当前实例不是锁{}的持有者，直接返回",lockName);
                continue;
            }
            log.info("【LockHeartServer】当前实例是锁{}的持有者，检查超时",lockName);
            LockHeart lockHeart = lockInfo.getLockHeart();
            // 心跳时间差（秒）
           long currentTimeMillis=System.currentTimeMillis();
            long heartTime = (currentTimeMillis - lockHeart.getLastTime()) / 1000;
            // 获取锁超时时间
            long lockTimeOut = lockServer.getLockTimeOut(lockInfo);
            log.info("【LockHeartServer】当前时间为："+currentTimeMillis);
            log.info("【LockHeartServer】锁的最后刷新时间："+lockHeart.getLastTime());
            log.info("【LockHeartServer】持续未刷新时间："+heartTime);
            log.info("【LockHeartServer】锁的超时时间为："+lockTimeOut);
            if (heartTime >= lockTimeOut) {
                log.error("【LockHeartServer】锁【{}】心跳超时。心跳刷新日志：{}", lockName, refreshLockHeartLogMap);
                // 清空心跳刷新日志
                refreshLockHeartLogMap.clear();
                // 释放锁
                try {
                    lockServer.releaseLock(lockName);
                    log.error("【LockHeartServer】锁【{}】心跳超时，释放锁成功", lockName);
                } catch (Exception e) {
                    log.error("释放锁【{}】失败 Message:{}", lockName, Common.getStackTrace(e));
                    // 通知失去锁
                    HEART_THREAD_POOL.submit(new Runnable() {
                        @Override
                        public void run() {
                            lockInfo.getLockNotify().notLeader(HEART_THREAD_POOL);
                        }
                    });
                }
            }
        }
    }

    /**
     * 更新心跳(判断是否是主机)
     */
    void updateLockHeart(String lockName, LockServer lockServer) {
        // 向服务器确认是否是主机如果网络异常有可能导致锁超时 所以只在内存校验
        LockInfo lockInfo = lockServer.getLockInfoMap().get(lockName);
        // 判断是否已经通知过失去锁
        if (lockInfo == null || !lockInfo.isLockLeader()) {
            return;
        }
        // 更新心跳时间
        log.info("【LockHeartServer】通过updateLockHeart刷新了心跳");
        lockInfo.getLockHeart().refreshLastTime();
        // 记录日志
        Thread thread = Thread.currentThread();
        String threadName = thread.getName();
        StackTraceElement[] stackTraces = thread.getStackTrace();
        List<RefreshLockHeartLog> refreshLockHeartLogList = refreshLockHeartLogMap.get(threadName);
        if (refreshLockHeartLogList == null) {
            refreshLockHeartLogList = new ArrayList<>();
        }
        refreshLockHeartLogList.add(0, new RefreshLockHeartLog(stackTraces[4].toString()));
        // 记录保留10条数据
        if (refreshLockHeartLogList.size() > 10) {
            refreshLockHeartLogList = refreshLockHeartLogList.subList(0, 10);
        }
        refreshLockHeartLogMap.put(threadName, refreshLockHeartLogList);
    }

    /**
     * 关闭ZkClient
     */
    void close() {
        if (lockHeartScheduledThreadPool != null) {
            lockHeartScheduledThreadPool.shutdown();
        }
    }

    /**
     * 刷新心跳日志
     */
    @Data
    static class RefreshLockHeartLog {

        private String stackTracePath;

        private LocalDateTime refreshTime;

        public RefreshLockHeartLog(String stackTracePath) {
            this.stackTracePath = stackTracePath;
            this.refreshTime = LocalDateTime.now();
        }
    }
}
