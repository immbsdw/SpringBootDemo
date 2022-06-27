//package cn.com.demo.zookeeper;
//
//import cn.com.demo.comm.Common;
//import cn.com.demo.zookeeper.callback.IZookeeperConfigWatchNotify;
//import cn.com.demo.zookeeper.callback.impl.ZookeeperLockWatchNotify;
//import cn.com.demo.zookeeper.callback.impl.ZookeeperServerConfigWatchNotify;
//import cn.com.demo.zookeeper.constant.ThreadPoolConstant;
//import cn.com.demo.zookeeper.constant.ZKParamConstant;
//import cn.com.demo.zookeeper.exception.ConfigWatchException;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.curator.framework.CuratorFramework;
//import org.apache.curator.framework.recipes.cache.NodeCache;
//import org.apache.curator.framework.recipes.cache.PathChildrenCache;
//import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
//import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.Closeable;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ThreadFactory;
//
//@Slf4j
//class ConfigWatcherServer {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigWatcherServer.class);
//
//    /**
//     * ZK客户端
//     */
//    private CuratorFramework client;
//
//    private NodeServer nodeServer;
//
//    /**
//     * 当前ZKClient实例所有监控
//     */
//    private Map<String, Closeable> watchMap = new ConcurrentHashMap<>();
//
//    /**
//     * 锁节点监控线程池
//     */
//    public ExecutorService lockWatchThreadPool;
//
//    public ConfigWatcherServer(CuratorFramework client, NodeServer nodeServer, ZkClient zkClient) {
//        this.client = client;
//        this.nodeServer = nodeServer;
//        // 创建锁节点监控线程池
//        this.lockWatchThreadPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
//            @Override
//            public Thread newThread(Runnable r) {
//                Thread thread = new Thread(r);
//                thread.setDaemon(true);
//                thread.setName("锁节点监控线程池");
//                return thread;
//            }
//        });
//        // 开启锁控节点监(当锁被删除时退出主机)
//        this.registerLockNotify(new ZookeeperLockWatchNotify(), zkClient);
//        // 监控zookeeper地址节点(当ZK服务器地址变动时修改本地配置文件)
//        this.registerConfigNotify(ZKParamConstant.CONFIG_ZKSERVER_NODENAME, new ZookeeperServerConfigWatchNotify());
//    }
//
//    /**
//     * 锁监控
//     */
//    synchronized void registerLockNotify(ZookeeperLockWatchNotify lockWatchNotify, ZkClient zkClient) {
//        // 监控锁节点
//        final PathChildrenCache watcher = new PathChildrenCache(this.client, ZKParamConstant.LOCK_NODENAME, true, false, lockWatchThreadPool);
//        try {
//            // 开启新监听
//            watcher.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
//        } catch (Exception e) {
//            LOGGER.error("锁监听失败 Message:{}", Common.getStackTrace(e));
//            throw new ConfigWatchException(e.getMessage(), e);
//        }
//        // 添加回调
//        watcher.getListenable().addListener(new PathChildrenCacheListener() {
//            @Override
//            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) {
//                try {
//                    lockWatchNotify.onChange(event, zkClient);
//                } catch (Exception | Error e) {
//                    log.error("锁监控回调错误:{}", Common.getStackTrace(e));
//                }
//            }
//        }, lockWatchThreadPool);
//        // 添加监控列表
//        watchMap.put(ZKParamConstant.LOCK_NODENAME, watcher);
//    }
//
//    /**
//     * 注册配置监控
//     */
//    synchronized void registerConfigNotify(String nodeName, IZookeeperConfigWatchNotify configCallBack) {
//        String path = ZKParamConstant.getNodeName(nodeName);
//        final NodeCache watcher = new NodeCache(this.client, path);
//        try {
//            // 关闭原有的监听
//            this.quitConfigNotify(nodeName);
//            // 开启新监听
//            watcher.start(false);
//        } catch (Exception e) {
//            LOGGER.error("节点【{}】监听失败 Message:{}", nodeName, Common.getStackTrace(e));
//            throw new ConfigWatchException(e.getMessage(), e);
//        }
//        // 第一次监听节点不存在返回null
//        if (!nodeServer.checkNodeExist(path)) {
//            try {
//                configCallBack.onChanged(nodeName, null, null);
//            } catch (Exception | Error e) {
//                log.error("配置监控回调错误:{}", Common.getStackTrace(e));
//            }
//        }
//        // 添加监听
//        watcher.getListenable().addListener(() -> {
//            try {
//                String oldData = nodeServer.getNodeData(ZKParamConstant.getHistoryNodeName(path));
//                String data;
//                if (watcher.getCurrentData() == null) {
//                    data = null;
//                } else {
//                    data = new String(watcher.getCurrentData().getData(), StandardCharsets.UTF_8);
//                }
//                // 发送回调
//                configCallBack.onChanged(nodeName, data, oldData);
//            } catch (Exception | Error e) {
//                log.error("配置监控回调错误:{}", Common.getStackTrace(e));
//            }
//        }, ThreadPoolConstant.CONFIG_WATCH_THREAD_POOL);
//        // 添加监控列表
//        watchMap.put(path, watcher);
//    }
//
//
//    /**
//     * 退出配置监控
//     */
//    void quitConfigNotify(String nodeName) throws Exception {
//        String path = ZKParamConstant.getNodeName(nodeName);
//        Closeable closeable = watchMap.get(path);
//        if (closeable != null) {
//            closeable.close();
//            // 删除监控集合
//            watchMap.remove(path);
//        }
//    }
//
//    /**
//     * 关闭client
//     */
//    void close() throws IOException {
//        for (Closeable closeable : watchMap.values()) {
//            closeable.close();
//        }
//        watchMap.clear();
//        if (lockWatchThreadPool != null) {
//            lockWatchThreadPool.shutdown();
//        }
//    }
//
//}
