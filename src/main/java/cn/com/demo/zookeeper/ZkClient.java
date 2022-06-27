package cn.com.demo.zookeeper;

import cn.com.demo.comm.Common;
import cn.com.demo.zookeeper.callback.IZookeeperConfigWatchNotify;
import cn.com.demo.zookeeper.callback.IZookeeperLockNotify;
import cn.com.demo.zookeeper.constant.ZKParamConstant;
import cn.com.demo.zookeeper.constant.ZkServerAreaEnum;
import cn.com.demo.zookeeper.domain.ClientInfo;
import cn.com.demo.zookeeper.domain.LockNodeInfo;
import cn.com.demo.zookeeper.exception.ZooKeeperClientIdentityException;
import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.RetryUntilElapsed;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 *
 * 单例的zk客户端
 */
@Slf4j
public class ZkClient {

    /**
     * curator实例
     */
    private CuratorFramework client;

    /**
     * 客户端标识
     */
    @Getter
    private String clientId;

    /**
     * 节点操作
     */
    private NodeServer nodeServer;

//    /**
//     * 配置监听
//     */
//    private ConfigWatcherServer configWatcherServer;

    /**
     * 锁
     */
    private LockServer lockServer;

    /**
     * 单例
     */
    public static ZkClient getInstance() {
        return InnerClass.zkClient;
    }

    private static class InnerClass {
        private static ZkClient zkClient = new ZkClient();
    }

    private ZkClient() {
        this.init(ZKParamConstant.priorityZkServer);
    }

    /**
     * 由ZKClientManager调用
     *
     * @param zkServerAreaEnum ZK服务器地址
     */
    ZkClient(ZkServerAreaEnum zkServerAreaEnum) {
        try {
            this.init(zkServerAreaEnum);
        } catch (Exception e) {
            this.client.close();
            throw e;
        }
    }

    /**
     * 初始化
     */
    private void init(ZkServerAreaEnum zkServerArea) {
        // 1.链接ZK服务器，创建CuratorFramework对象实例，并启动该实例
        this.client = this.connectZkServer(zkServerArea);
        // 2.创建NodeServer实例，并测试zk连接是否成功，NodeServer类定义了节点的操作
        this.nodeServer = new NodeServer(this.client).check();
        // 3.判断是否有连接权限
        this.checkClientIdentity();
        // 4.创建锁服务实例，同时启动LockHeartServer（用于刷新心跳确认当前调度中心正在正常运行）和LockCheckServer（用于保持本地缓存信息与zookepeer节点信息的一致）
        this.lockServer = new LockServer(this.client, this.nodeServer);
        // 5.创建监控实例
        //dw:创建processor/lock节点
//        this.configWatcherServer = new ConfigWatcherServer(this.client, this.nodeServer, this);
    }

    /**
     * 创建zkClient实例
     */
    private CuratorFramework connectZkServer(ZkServerAreaEnum zkServerArea) {
        // 获取zk服务器地址
        String zkServerPath = ZKParamConstant.zkServerMap.get(zkServerArea);
        // 重连策略
        RetryUntilElapsed retryPolicy = new RetryUntilElapsed(10 * 1000, 2 * 1000);
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(zkServerPath)
                .retryPolicy(retryPolicy)
                // 会话超时
                .sessionTimeoutMs(ZKParamConstant.SESSION_TIMEOUTMS * 1000)
                // 连接超时
                .connectionTimeoutMs(ZKParamConstant.CONNECTION_TIMEOUTMS * 1000)
                .namespace("processor")
                .build();
        client.start();
        return client;
    }

    /**
     * 判断是否有连接权限
     */
    private void checkClientIdentity() {
        if (!ZKParamConstant.checkClientIdentity) {
            final ClientInfo clientInfo = new ClientInfo();
            // 获取客户端信息
            String config = this.getConfig(ZKParamConstant.CONFIG_CLIENT_NODENAME);
            if (config != null) {
                List<ClientInfo> list = JSON.parseArray(config, ClientInfo.class);
                int i = list.indexOf(clientInfo);
                if (i >= 0) {
                    this.clientId = list.get(i).getClientId();
                    return;
                }
                throw new ZooKeeperClientIdentityException("zk客户端没有连接权限");
            }
        }
        // 生成clientId 机器名（IP）
        try {
            //本地测试给IP加一个随机数
            this.clientId = InetAddress.getLocalHost().toString()+"."+ new Random().nextInt(1000);
        } catch (UnknownHostException e) {
            this.clientId = "获取机器信息失败";
        }
    }

    /**
     * 注册锁变更通知
     *
     * @param lockName 锁名称
     * @return 是否成功
     */
    public boolean registerLockNotify(String lockName, IZookeeperLockNotify zookeeperLockNotify) {
        // 获取锁默认超时时间
        long timeOut = lockServer.getDefaultLockTimeOut();
        return registerLockNotify(lockName, zookeeperLockNotify, timeOut);
    }

    /**
     * 注册锁变更通知
     *
     * @param lockName 锁名称
     * @return 是否成功
     */
    private boolean registerLockNotify(String lockName, IZookeeperLockNotify zookeeperLockNotify, long timeOut) {
        // zk锁节点名
        String lockNodeName = ZKParamConstant.getLockNodeName(lockName);
        try {
            // 添加锁失败
            //dw：创建/lock/ConverterSchedulerLock
            if (!createLock(lockName, timeOut)) {
                return false;
            }
        } catch (Exception e) {
            log.error("获取锁失败:{}", Common.getStackTrace(e));
        }
        // 获取锁
        //dw：通过LeaderLatch在/lock/ConverterSchedulerLock下创建临时节点，来实现选举出调度中心
        return lockServer.registerLock(lockName, zookeeperLockNotify, lockNodeName, clientId);
    }

    /**
     * 退出锁变更通知
     *
     * @param lockName 锁名称
     * @return 是否成功
     */
    public boolean quitLockNotify(String lockName) {
        try {
            lockServer.quitLock(lockName);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 释放锁重新抢主
     *
     * @param lockName 锁名称
     * @return 是否成功
     */
    public boolean releaseLock(String lockName) {
        try {
            Boolean hasLock = this.hasLock(lockName);
            if (hasLock) {
                lockServer.releaseLock(lockName);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 判断自己是否拥有锁
     *
     * @param lockName 锁名称
     * @return 是否是主机
     */
    public Boolean hasLock(String lockName) {
        return lockServer.hasLock(lockName);
    }

    /**
     * 添加锁（抢主时会自动创建）
     * 如果没有就添加
     */
    private boolean createLock(String lockName, long timeOut) {
        String lockNodeName = ZKParamConstant.getLockNodeName(lockName);
        LockNodeInfo lockNodeInfo;
        if (!nodeServer.checkNodeExist(lockNodeName)) {
            lockNodeInfo = new LockNodeInfo(lockName, timeOut);
            return nodeServer.createPersistentNode(lockNodeName, JSON.toJSONString(lockNodeInfo));
        }
        return true;
    }

    /**
     * 删除锁
     *
     * @param lockName 锁名称
     * @return 是否成功
     */
    public boolean deleteLock(String lockName) {
        String lockNodeName = ZKParamConstant.getLockNodeName(lockName);
        return nodeServer.deleteNode(lockNodeName);
    }

    /**
     * 更新锁心跳
     */
    public void updateLockHeart(String lockName) {
        lockServer.updateLockHeart(lockName);
    }

//    /**
//     * 注册配置变更通知
//     */
//    public boolean registerConfigNotify(String nodeName, IZookeeperConfigWatchNotify configCallBack) {
//        try {
//            configWatcherServer.registerConfigNotify(nodeName, configCallBack);
//        } catch (Exception e) {
//            log.error("监控【{}】失败:{}", nodeName, Common.getStackTrace(e));
//            return false;
//        }
//        return true;
//    }

//    /**
//     * 退出配置变更通知
//     */
//    public boolean quitConfigNotify(String nodeName) {
//        try {
//            configWatcherServer.quitConfigNotify(nodeName);
//        } catch (Exception e) {
//            log.error("关闭监控失败:{}", Common.getStackTrace(e));
//            return false;
//        }
//        return true;
//    }

    /**
     * 获取节点数据
     */
    public String getConfig(String nodeName) {
        return nodeServer.getNodeData(nodeName);
    }

    /**
     * 获取节点下的所有子节点数据
     */
    public Map<String, String> getChildrenConfig(String path) {
        return nodeServer.getChildrenNodeData(path);
    }

    /**
     * 获取节点下的所有子节点
     */
    public List<String> getChildrenConfigName(String path) {
        return nodeServer.getChildrenNode(path);
    }

    /**
     * 获取节点数据（带默认值）
     */
    public String getConfig(String nodeName, String defaultData) {
        String data = getConfig(nodeName);
        return data == null ? defaultData : data;
    }

    /**
     * 创建配置节点
     */
    public boolean createConfig(String nodeName, String data) {
        data = data == null ? "" : data;
        String oldData = nodeServer.getNodeData(nodeName);
        oldData = oldData == null ? data : oldData;
        return nodeServer.createPersistentNode(ZKParamConstant.getHistoryNodeName(nodeName), oldData) &&
                nodeServer.createPersistentNode(nodeName, data);
    }

    /**
     * 创建配置节点(临时节点)
     */
    public boolean createConfigEphemeral(String nodeName, String data) {
        data = data == null ? "" : data;
        String oldData = nodeServer.getNodeData(nodeName);
        oldData = oldData == null ? data : oldData;
        return nodeServer.createEphemeralNode(ZKParamConstant.getHistoryNodeName(nodeName), oldData) &&
                nodeServer.createEphemeralNode(nodeName, data);
    }

    /**
     * 修改配置节点
     */
    public boolean updateConfig(String nodeName, String data) {
        return createConfig(nodeName, data);
    }

    /**
     * 修改临时节点
     */
    public boolean updateConfigEphemeral(String nodeName, String data) {
        return createConfigEphemeral(nodeName, data);
    }

    /**
     * 删除配置节点
     *
     * @param nodeName 节点名称
     * @return 是否成功
     */
    public boolean deleteConfig(String nodeName) {
        return nodeServer.deleteNode(nodeName);
    }

    /**
     * 关闭zkClient实例
     */
    public void close() throws IOException {
        log.info("关闭ZKClient客户端");
        // 销毁锁实例
        lockServer.close();
        // 销毁监控实例
//        configWatcherServer.close();
        // 关闭客户端
        client.close();
    }

    /**
     * 返回连接状态
     */
    public CuratorFrameworkState getState() {
        return client.getState();
    }
}
