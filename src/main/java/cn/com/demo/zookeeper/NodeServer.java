package cn.com.demo.zookeeper;

import cn.com.demo.comm.Common;
import cn.com.demo.zookeeper.constant.ZKParamConstant;
import cn.com.demo.zookeeper.exception.ZooKeeperConnectException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点操作
 */
public class NodeServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeServer.class);

    /**
     * ZK客户端
     */
    private CuratorFramework client;

    public NodeServer(CuratorFramework client) {
        this.client = client;
    }

    /**
     * 判断连接是否成功
     */
    NodeServer check() {
        try {
            this.getNodeData("test");
        } catch (Exception e) {
            throw new ZooKeeperConnectException("zk集群连接失败", e);
        }
        return this;
    }

    /**
     * 添加临时节点(有则修改节点)
     */
    boolean createEphemeralNode(String nodeName, String data) {
        nodeName = ZKParamConstant.getNodeName(nodeName);
        // 节点已存在
        if (checkNodeExist(nodeName)) {
            return updateNode(nodeName, data);
        }
        try {
            this.client.create()
                    .creatingParentContainersIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(nodeName, data.getBytes());
        } catch (Exception e) {
            LOGGER.error("添加临时节点异常 Message:{}", Common.getStackTrace(e));
            return false;
        }
        return true;
    }

    /**
     * 添加永久节点(有则修改节点)
     */
    public boolean createPersistentNode(String nodeName, String data) {
        nodeName = ZKParamConstant.getNodeName(nodeName);
        // 节点已存在
        if (checkNodeExist(nodeName)) {
            return updateNode(nodeName, data);
        }
        try {
            this.client.create()
                    .creatingParentContainersIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(nodeName, data.getBytes());
        } catch (Exception e) {
            LOGGER.error("添加永久节点异常 Message:{}", Common.getStackTrace(e));
            return false;
        }
        return true;
    }

    /**
     * 删除节点
     */
    boolean deleteNode(String nodeName) {
        nodeName = ZKParamConstant.getNodeName(nodeName);
        // 节点不存在
        if (!checkNodeExist(nodeName)) {
            return false;
        }
        try {
            this.client.delete().deletingChildrenIfNeeded().forPath(nodeName);
        } catch (Exception e) {
            LOGGER.error("删除节点异常 Message:{}", Common.getStackTrace(e));
            return false;
        }
        return true;
    }

    /**
     * 修改节点
     */
    boolean updateNode(String nodeName, String data) {
        nodeName = ZKParamConstant.getNodeName(nodeName);
        // 节点不存在
        if (!checkNodeExist(nodeName)) {
            return false;
        }
        try {
            this.client.setData().forPath(nodeName, data.getBytes());
        } catch (Exception e) {
            LOGGER.error("修改节点数据异常 Message:{}", Common.getStackTrace(e));
            return false;
        }
        return true;
    }

    /**
     * 获取子节点的所有数据
     */
    Map<String, String> getChildrenNodeData(String nodeName) {
        nodeName = ZKParamConstant.getNodeName(nodeName);
        // 节点不存在
        if (!checkNodeExist(nodeName)) {
            return null;
        }
        Map<String, String> nodeMap = new HashMap<>();
        try {
            List<String> childrenNodeList = this.client.getChildren().forPath(nodeName);
            // 去除最后一个/
            if (nodeName.endsWith("/")) {
                nodeName = nodeName.substring(0, nodeName.length() - 1);
            }
            for (String childrenNode : childrenNodeList) {
                nodeMap.put(childrenNode, new String(this.client.getData().forPath(nodeName + "/" + childrenNode), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            LOGGER.error("获取子节点所有数据异常 Message:{}", Common.getStackTrace(e));
            throw new ZooKeeperConnectException(e.getMessage(), e);
        }
        return nodeMap;
    }

    /**
     * 获取子节点的所有数据
     */
    List<String> getChildrenNode(String nodeName) {
        nodeName = ZKParamConstant.getNodeName(nodeName);
        // 节点不存在
        if (!checkNodeExist(nodeName)) {
            return null;
        }
        try {
            return this.client.getChildren().forPath(nodeName);
        } catch (Exception e) {
            LOGGER.error("获取子节点所有数据异常 Message:{}", Common.getStackTrace(e));
            throw new ZooKeeperConnectException(e.getMessage(), e);
        }
    }

    /**
     * 获取单节点数据
     */
    public String getNodeData(String nodeName) {
        return getNodeData(this.client, nodeName);
    }

    /**
     * 获取单节点数据
     */
    private String getNodeData(CuratorFramework client, String nodeName) {
        nodeName = ZKParamConstant.getNodeName(nodeName);
        // 节点不存在
        if (!checkNodeExist(nodeName)) {
            return null;
        }
        try {
            byte[] bytes = this.client.getData().forPath(nodeName);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("获取单节点数据异常 Message:{}", Common.getStackTrace(e));
            throw new ZooKeeperConnectException(e.getMessage(), e);
        }
    }

    /**
     * 判断节点是否存在
     */
    boolean checkNodeExist(String nodeName) {
        try {
            Stat stat = this.client.checkExists().forPath(nodeName);
            return stat != null;
        } catch (Exception e) {
            LOGGER.error("判断节点是否存在异常 Message:{}", Common.getStackTrace(e));
            throw new ZooKeeperConnectException(e.getMessage(), e);
        }
    }

}
