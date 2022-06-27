package cn.com.demo.zookeeper.test.testselection;

import cn.com.demo.comm.Common;
import cn.com.demo.zookeeper.ZkClient;
import cn.com.demo.zookeeper.test.ZookeperTest;

/**
 * 调度中心，
 * 先启动，变成调度中心
 */
public class LeaderTest {

    //（1）抢占锁成功成为调度中心，抢占锁失败普通实例
    //(2) 调度中心异常，重新选举出新的调度中心，
    //(3) 普通实例异常，
    public static void main(String[] args) {
        ZkClient zkClient = ZkClient.getInstance();
        String Conver_zklock_converterSchedulerLockName="ConverterSchedulerLock";
        ZookeperTest.ZookeeperLockNotifyImpl zookeeperLockNotify=new ZookeperTest.ZookeeperLockNotifyImpl();
        zkClient.registerLockNotify(Conver_zklock_converterSchedulerLockName,zookeeperLockNotify);
        for(int i=0;;i++){
            System.out.println("pause");
            Common.sleep(1000);
//            zkClient.updateLockHeart(Conver_zklock_converterSchedulerLockName);
        }
    }
}
