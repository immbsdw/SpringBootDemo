package cn.com.demo.zookeeper.callback.impl;


import cn.com.demo.comm.Common;
import cn.com.demo.zookeeper.LockServer;
import cn.com.demo.zookeeper.NodeServer;
import cn.com.demo.zookeeper.callback.IZookeeperLockNotify;
import cn.com.demo.zookeeper.constant.ZKParamConstant;
import cn.com.demo.zookeeper.domain.LockInfo;
import cn.com.demo.zookeeper.domain.LockNodeInfo;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static cn.com.demo.zookeeper.constant.ThreadPoolConstant.LOCK_THREAD_POOL;


/**
 * 单个抢锁通知处理
 * dw:对应单个锁的处理
 */
@Slf4j
public class ZookeeperLockCallBackNotify implements LeaderLatchListener {

    private NodeServer nodeServer;

    private LockServer lockServer;

    /**
     * 锁名称
     */
    private String lockName;

    /**
     * 通知回调
     */
    private IZookeeperLockNotify zookeeperLockNotify;

    private ReentrantLock reentrantLock = new ReentrantLock();

    public ZookeeperLockCallBackNotify(String lockName, IZookeeperLockNotify zookeeperLockNotify, LockServer lockServer, NodeServer nodeServer) {
        this.lockName = lockName;
        this.zookeeperLockNotify = zookeeperLockNotify;
        this.lockServer = lockServer;
        this.nodeServer = nodeServer;
    }

    /**
     * ZK获得锁回调
     */
    @Override
    public void isLeader() {
        log.info("【ZookeeperLockCallBackNotify】当前实例被选举为Leader，开始执行isLeader");
        this.isLeader(LOCK_THREAD_POOL);
    }

    public void isLeader(ExecutorService executor) {
        try {
            reentrantLock.lock();
            log.info("【ZookeeperLockCallBackNotify】开始执行isLeader具体方法，提交异步任务执行，并设置超时时间防止卡死，leader的业务");
            // 使用future并设置超时时间,防止该线程因网络原因卡主,导致没有实例被通知抢到锁
            Future<Void> future = executor.submit(new Callable<Void>() {
                @Override
                public Void call() {
                    // 开始处理时间
                    LocalDateTime startTime = LocalDateTime.now();
                    // 最大等待通知时间
                    long lastNotifyTime = ZKParamConstant.GETLOCK_CALLBACK_DELAY;
                    while (true) {
                        LockInfo lockInfo = lockServer.getLockInfoMap().get(lockName);
                        if (lockInfo == null) {
                            log.error("ZK获得锁回调-获得锁【{}】出错 不在抢锁列表中", lockName);
                            return null;
                        }
                        if (!lockServer.hasLock(lockName)) {
                            log.warn("ZK获得锁回调-取消回调:在处理时失去锁【{}】", lockName);
                            return null;
                        }
                        if (lockInfo.isLockLeader()) {
                            log.warn("ZK获得锁回调-取消回调:已通知过获得锁【{}】", lockName);
                            return null;
                        }
                        // 获取当前锁节点信息
                        LockNodeInfo lockNodeInfo = null;
                        // 判断是否超过最大等待通知时间
                        try {
                            //访问zk获取节点信息
                            String  oldLockInfoStr=nodeServer.getNodeData(lockInfo.getLockPath());
                            //当前实例抢占到锁，获取node的value，即上一个获取老的lockInfo
                            lockNodeInfo = JSON.parseObject(oldLockInfoStr, LockNodeInfo.class);
                            //dw：（1）当前节点被选举为Leader（第一次选举，或重新选举）时，触发修改节点中value（LockInfo对象）逻辑
                            //dw：（2）当前节点被选为Leader存在的情况：
                            //dw：    上一个Leader调用LeaderLatch的close()方法正常退出（锁节点中的endTime不为0且lockLeader不为空）；
                            //dw：    上一个节点异常退出（锁节点中的endTime为0且lockLeader不为空）；
                            //dw：    第一次进行leader选举（锁节点的整个对象value为空）；
                            //dw:（3）去修改锁节点的Value(lockInfo)的三种情况：
                            //dw:     上个Leader挂了：检测了lastNotifyTime（20s）每秒钟进行一次，且每次锁节点的数据中endTime都为0，认为上个leader挂了；
                            //dw:     检测到锁被主动释放了（即当锁节点的数据中endTime不为0）；
                            //dw:     检测到lockInfo为空则认为是第一次进行Leader选举
                            if (Duration.between(startTime, LocalDateTime.now()).getSeconds() < lastNotifyTime) {
                                // 判断上一个抢到锁的服务是否已接收到失去通知
                                //dw：即上个获取锁的服务是否是主动的调了latch.close()，触发了noleader（）方法将endTime设置为非0
                                if (lockNodeInfo.getLockLeader() != null && lockNodeInfo.getEndTime() == 0) {
                                    log.info("【ZookeeperLockCallBackNotify】上个Leader任未主动释放节点并将endTime设为非0");
                                    Common.sleep(1000);
                                    continue;
                                }
                            }
                        } catch (Exception e) {
                            log.error("锁节点:{} 数据解析出错:{}", lockName, e.getMessage());
                            // 如果锁节点数据解析出错，休眠最大等待通知时间剩余的时间
                            LocalDateTime localDateTime = startTime.plusSeconds(lastNotifyTime);
                            Common.sleep(Duration.between(startTime, localDateTime).getSeconds() * 1000);
                        }
                        // 解析失败重新创建对象
                        if (lockNodeInfo == null) {
                            // 获取锁默认超时时间
                            long timeOut = lockServer.getDefaultLockTimeOut();
                            lockNodeInfo = new LockNodeInfo(lockName, timeOut);
                        }
                        // dw：当前一个线程成功的释放锁后，更新锁节点信息
                        lockNodeInfo.setStartTime(System.currentTimeMillis());
                        lockNodeInfo.setEndTime(0);
                        lockNodeInfo.setLockLeader(lockInfo.getClientId());
                        //dw：修改锁节点的value
                        log.info("【ZookeeperLockCallBackNotify】开始创建或修改锁节点"+lockName+"，设置将value设置为的"+JSON.toJSONString(lockNodeInfo));
                        boolean updateNodeSuccess = nodeServer.createPersistentNode(lockInfo.getLockPath(), JSON.toJSONString(lockNodeInfo));
                        if (!updateNodeSuccess) {
                            log.error("ZK获得锁回调-更新锁【{}】信息出错", lockName);
                            try {
                                // 更新锁节点信息异常-释放当前锁
                                lockServer.releaseLock(lockName);
                            } catch (IOException e) {
                                log.error("释放锁【{}】失败 Message:{}", lockName, Common.getStackTrace(e));
                            }
                            return null;
                        }
                        // dw:只更新本地缓存中即LockInfo.lockHeart中的心跳时间，不修改zk节点中的数据LockNodeInfo
                        log.info("【ZookeeperLockCallBackNotify】isLeader中刷新了心跳");
                        lockInfo.getLockHeart().refreshLastTime();
                        // 通知过主机
                        //dw:当前实例被选举为Leader并且修改LockNodeInfo成功
                        lockInfo.setLockLeader(true);
                        // 异步通知ZKClient客户端
                        executor.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    //dw:当前实例被选举为Leader后，异步触发Leader的业务逻辑
                                    log.info("【IZookeeperLockNotify】ZK获得锁即成为Leader时，开始异步运行Leader的业务逻辑");
                                    zookeeperLockNotify.onEntryLock(lockName);
                                } catch (Exception | Error e) {
                                    log.error("ZK获得锁回调-ZKClient客户端执行获得锁【{}】回调出错 Message:{}", lockName, Common.getStackTrace(e));
                                }
                            }
                        });
                        break;
                    }
                    return null;
                }
            });
            // 同步执行future
            try {
                //dw:异步任务（修改锁节点信息，启动Leader的业务逻辑）是否在指定时间内40s完成
                //dw:一个isLeader（）(或notLeader（）)方法对应的是对一个锁的通知处理，但可以有多个线程同时对锁进行操作，因此需要通过锁来保证线程安全
                //dw:本来应该是单线程安全的（顺序接收来自zk的关于该锁的变更，并进行处理）
                //dw:但是LockCheckServer中包含了对该方法的调用，定时检测该锁，并主动调用该方法来处理一些异常
                //dw：因此存在多个线程来调用该方法，curator框架中的接收通知的线程，及锁的定时检测线程
                future.get(ZKParamConstant.GETLOCK_CALLBACK_DELAY * 2, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("ZK获得锁回调-future执行异常! Message:{}", Common.getStackTrace(e));
                future.cancel(true);
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    /**
     * ZK失去锁回调
     */
    @Override
    public void notLeader() {
        log.info("【ZookeeperLockCallBackNotify】当前实例选举为Leader失败，开始执行notLeader");
        this.notLeader(LOCK_THREAD_POOL);
    }

    /**
     * ZK失去锁回调
     */
    public void notLeader(ExecutorService executor) {
        try {
            reentrantLock.lock();
            log.info("【ZookeeperLockCallBackNotify】开始执行notLeader具体方法，提交异步任务执行，并设置超时时间防止卡死，leader的业务");
            LockInfo lockInfo = lockServer.getLockInfoMap().get(lockName);
            if (lockInfo == null) {
                log.error("【ZookeeperLockCallBackNotify】ZK失去锁回调-失去锁【{}】出错 不在抢锁列表中", lockName);
                return;
            }
            if (!lockInfo.isLockLeader()) {
                log.warn("【ZookeeperLockCallBackNotify】ZK失去锁回调-取消回调:已通知过失去锁【{}】", lockName);
                return;
            }
            // 更新本次抢锁信息
            lockInfo.setLockLeader(false);
            // 异步通知ZKClient客户端
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        log.info("ZK失去锁回调-通知处理机");
                        zookeeperLockNotify.onExitLock(lockName);
                    } catch (Exception | Error e) {
                        log.error("ZK失去锁回调-ZKClient客户端执行失去锁【{}】回调出错 Message:{}", lockName, Common.getStackTrace(e));
                    }
                }
            });
            // 使用future并设置超时时间,防止该线程因网络原因卡主,导致没有实例被通知抢到锁
            Future<Void> future = executor.submit(new Callable<Void>() {
                @Override
                public Void call() {
                    // 获取当前锁节点信息
                    try {
                        LockNodeInfo lockNodeInfo;
                        String nodeData = nodeServer.getNodeData(lockInfo.getLockPath());
                        // 锁节点不存在
                        if (nodeData == null) {
                            throw new RuntimeException("锁节点不存在");
                        } else {
                            lockNodeInfo = JSON.parseObject(nodeData, LockNodeInfo.class);
                            // 更新锁节点信息
                            if (lockInfo.getClientId().equals(lockNodeInfo.getLockLeader())) {
                                lockNodeInfo.setEndTime(System.currentTimeMillis());
                                nodeServer.createPersistentNode(lockInfo.getLockPath(), JSON.toJSONString(lockNodeInfo));
                            }
                        }
                    } catch (Exception e) {
                        log.error("ZK失去锁回调-更新锁【{}】信息出错:{}", lockName, Common.getStackTrace(e));
                    }
                    return null;
                }
            });
            // 同步执行future
            try {
                future.get(ZKParamConstant.GETLOCK_CALLBACK_DELAY, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("ZK失去锁回调-future执行异常! Message:{}", Common.getStackTrace(e));
                future.cancel(true);
            }
        } finally {
            reentrantLock.unlock();
        }
    }
}
