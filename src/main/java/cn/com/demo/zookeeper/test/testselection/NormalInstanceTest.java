package cn.com.demo.zookeeper.test.testselection;

import cn.com.demo.comm.Common;
import cn.com.demo.zookeeper.ZkClient;
import cn.com.demo.zookeeper.test.ZookeperTest;

/**
 * 普通线程后启动，去抢占锁
 * 当调度中心下线后，重新选举出新的调度中心
 */
public class NormalInstanceTest {
    public static void main(String[] args) {
        ZkClient zkClient = ZkClient.getInstance();
        String Conver_zklock_converterSchedulerLockName="ConverterSchedulerLock";
        ZookeperTest.ZookeeperLockNotifyImpl zookeeperLockNotify=new ZookeperTest.ZookeeperLockNotifyImpl();
        for(int i=0;i<1;i++){
            new Thread(()->{
                zkClient.registerLockNotify(Conver_zklock_converterSchedulerLockName,zookeeperLockNotify);
                while (true){
                    System.out.println("pause");
                    Common.sleep(1000);
                    //刷新锁的心跳
                    zkClient.updateLockHeart(Conver_zklock_converterSchedulerLockName);
                }
            },"线程"+i).start();
        }
    }
}
