package cn.com.demo.zookeeper.callback.impl;

import cn.com.demo.zookeeper.ZkClient;
import cn.com.demo.zookeeper.domain.LockNodeInfo;
import com.alibaba.fastjson.JSON;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * 锁节点监控回调
 */
public class ZookeeperLockWatchNotify {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperLockWatchNotify.class);

    /**
     * 锁节点变化
     */
    public void onChange(PathChildrenCacheEvent event, ZkClient zkClient) {
        LockNodeInfo lockNodeInfo;
        String lockName;
        switch (event.getType()) {
            // 删除锁
            case CHILD_REMOVED:
                lockNodeInfo = JSON.parseObject(new String(event.getData().getData(), StandardCharsets.UTF_8), LockNodeInfo.class);
                lockName = lockNodeInfo.getLockName();
                LOGGER.info("【ZookeeperLockWatchNotify】锁【{}】被删除", lockName);
                // 退出锁
                if (!zkClient.quitLockNotify(lockName)) {
                    LOGGER.error("锁【{}】被删除 退出锁失败", lockName);
                }
                break;
            default:
        }
    }

}
