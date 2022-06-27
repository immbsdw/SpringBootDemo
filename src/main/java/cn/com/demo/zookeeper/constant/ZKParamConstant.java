package cn.com.demo.zookeeper.constant;

import cn.com.demo.comm.Common;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 参数常量
 */
public class ZKParamConstant {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZKParamConstant.class);

    private static Properties prop = new Properties();

    /**
     * 是否检测客户端身份识别
     */
    public static boolean checkClientIdentity;

    /**
     * 优先集群
     */
    public static ZkServerAreaEnum priorityZkServer;

    public static Map<ZkServerAreaEnum, String> zkServerMap = new ConcurrentHashMap<>();

    /**
     * 是否debug模式
     */
    private static final String IS_DEBUG = "isDebug";

    /**
     * ZK集群配置文件
     */
    private static final String ZOOKEEPER_CONFIG = "ZookeeperConfig.properties";

    /**
     * 优先zk集群
     */
    private static final String ZOOKEEPER_CONFIG_PRIORITY = "priorityZkServer";

    /**
     * 配置文件zk集群
     */
    private static final String ZOOKEEPER_CONFIG_ZKSERVER = "zkServer";

    /**
     * 默认心跳超时时间（秒）
     * dw：原3 * 60
     */
    public static final long HEART_TIMEOUT = 3 * 60;

    /**
     * 锁检查间隔（秒）
     */
    public static final long LOCK_CHECKTIME = 15;

    /**
     * 心跳检查间隔（秒）
     */
    public static final long HEART_CHECKTIME = 5;

    /**
     * 会话超时（秒）
     */
    public static final int SESSION_TIMEOUTMS = 10;

    /**
     * 连接超时（秒）
     */
    public static final int CONNECTION_TIMEOUTMS = 10;

    /**
     * 最大延时通知获得锁时间（秒）
     */
    public static final long GETLOCK_CALLBACK_DELAY = SESSION_TIMEOUTMS * 2;

    /**
     * 锁节点
     */
    public static final String LOCK_NODENAME = "/lock";

    /**
     * 历史节点
     */
    public static final String HISTORY_NODENAME = "history";

    /**
     * zk服务器配置节点
     */
    public static final String CONFIG_ZKSERVER_NODENAME = "zkServer";

    /**
     * 客户端列表节点
     */
    public static final String CONFIG_CLIENT_NODENAME = "client";

    static {
        try (InputStream inputStream = new FileInputStream(ZOOKEEPER_CONFIG)) {
            prop.load(inputStream);
            checkClientIdentity = Boolean.parseBoolean(prop.getProperty(IS_DEBUG));
            priorityZkServer = ZkServerAreaEnum.valueOf(prop.getProperty(ZOOKEEPER_CONFIG_PRIORITY));
            zkServerMap = JSON.parseObject(prop.getProperty(ZOOKEEPER_CONFIG_ZKSERVER), new TypeReference<Map<ZkServerAreaEnum, String>>() {
            });
        } catch (Exception e) {
            LOGGER.error("读取配置文件 {} 出错使用默认配置 {}", ZOOKEEPER_CONFIG, Common.getStackTrace(e));
        }
    }

    /**
     * 修改配置文件
     */
    public synchronized static void updateConfig(String name, Object value) {
        try (FileOutputStream outputStream = new FileOutputStream(ZOOKEEPER_CONFIG)) {
            prop.setProperty(name, value.toString());
            prop.store(outputStream, name);
        } catch (Exception e) {
            LOGGER.error("修改配置文件 {} 出错 {}", ZOOKEEPER_CONFIG, Common.getStackTrace(e));
        }
    }

    /**
     * 获取锁节点名
     *
     * @param lockName 锁名称
     */
    public static String getLockNodeName(String lockName) {
        return LOCK_NODENAME + "/" + lockName;
    }

    /**
     * 获取节点的历史节点名
     *
     * @param nodeName 节点名
     */
    public static String getHistoryNodeName(String nodeName) {
        return nodeName + "/" + HISTORY_NODENAME;
    }

    /**
     * 获取节点名
     */
    public static String getNodeName(String nodeName) {
        if (!nodeName.startsWith("/")) {
            nodeName = "/" + nodeName;
        }
        return nodeName;
    }
}
