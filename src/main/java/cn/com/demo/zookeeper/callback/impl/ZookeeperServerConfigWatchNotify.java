package cn.com.demo.zookeeper.callback.impl;

import cn.com.demo.zookeeper.callback.IZookeeperConfigWatchNotify;
import cn.com.demo.zookeeper.constant.ZKParamConstant;
import cn.com.demo.zookeeper.constant.ZkServerAreaEnum;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * zk集群配置监控
 */
public class ZookeeperServerConfigWatchNotify implements IZookeeperConfigWatchNotify {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperServerConfigWatchNotify.class);

    /**
     * 集群变化
     */
    @Override
    public void onChanged(String config, String newData, String oldData) {
        if (newData == null) {
            return;
        }
        ZKParamConstant.zkServerMap = JSON.parseObject(newData, new TypeReference<Map<ZkServerAreaEnum, String>>() {
        });
        ZKParamConstant.updateConfig(config, newData);
    }
}
