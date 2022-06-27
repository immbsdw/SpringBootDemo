package cn.com.demo.zookeeper.domain;

import lombok.Data;

/**
 * 锁节点信息,zk中节点的value对象
 */
@Data
public class LockNodeInfo {

    /**
     * 锁名称
     */
    private String lockName;

    /**
     * 当前执行的主机
     */
    private String lockLeader;

    /**
     * 开始执行的时间
     */
    private long startTime;

    /**
     * 结束执行的时间
     * noleader中将其置为非0，isleader中将其设为0
     * 因此可知，如果在zookeper的节点上如果endTime为0表示该节点仍然被持有，如果不为0表示该锁被释放（leaderlatch.close()）的时间
     */
    private long endTime;


    /**
     * 超时时间
     */
    private long timeOut;

    public LockNodeInfo(String lockName, long timeOut) {
        this.lockName = lockName;
        this.timeOut = timeOut;
    }

    public LockNodeInfo(String lockName, String lockLeader, long startTime, long endTime, long timeOut) {
        this.lockName = lockName;
        this.lockLeader = lockLeader;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timeOut = timeOut;
    }

}

