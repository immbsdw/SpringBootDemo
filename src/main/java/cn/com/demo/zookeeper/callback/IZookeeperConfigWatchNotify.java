package cn.com.demo.zookeeper.callback;

/**
 * 配置监控回调
 */
public interface IZookeeperConfigWatchNotify {

    /**
     * 节点变化
     */
    void onChanged(String nodeName, String newData, String oldData);

}
