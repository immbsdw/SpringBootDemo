package cn.com.demo.zookeeper.test;

import cn.com.demo.comm.Common;
import cn.com.demo.comm.SuperJson;
import cn.com.demo.zookeeper.ZkClient;
import cn.com.demo.zookeeper.callback.IZookeeperLockNotify;
import cn.com.demo.zookeeper.domain.RpcServerAddress;

public class ZookeperTest {
   public static class ZookeeperLockNotifyImpl implements IZookeeperLockNotify{

       @Override
       public void onEntryLock(String lockName) {
           System.out.println("【ZookeeperLockNotifyImpl】"+Thread.currentThread().getName()+"获取到锁"+lockName);
       }

       @Override
       public void onExitLock(String lockName) {
           System.out.println("【ZookeeperLockNotifyImpl】"+Thread.currentThread().getName()+"失去锁"+lockName);
       }
   }

    public static void main(String[] args) {
        ZkClient zkClient = ZkClient.getInstance();
//        String zkNodeName = "DataDicServer/dataDic";
//        String ip="127.0.0.1";
//        int port= 2881;
//        String path="dataDic";
//        boolean successRegister=zkClient.createConfigEphemeral(zkNodeName, SuperJson.toJSONString(new RpcServerAddress(ip,port,path)));
//        System.out.println(successRegister);

//
//        //获取
//        String Conver_zkconfig_RpcPortName="PRCServerPort";
//        zkClient.getConfig(Conver_zkconfig_RpcPortName);
//
//        //注册服务
//        String Conver_zkconfig_RPCServerAddress="PRCServerAddress";
//        RpcServerAddress rpcServerAddress = new RpcServerAddress(ip,port, "/ConverterScheduler");
//        zkClient.createConfig(Conver_zkconfig_RPCServerAddress,rpcServerAddress.getPath());

        //（1）抢占锁成功成为调度中心，抢占锁失败普通实例
        //(2) 调度中心异常，重新选举出新的调度中心，
        //(3) 普通实例异常，
        //(4) 调度中心要不断去刷新心跳，当普通实例检测到超时，则抢锁
        String Conver_zklock_converterSchedulerLockName="ConverterSchedulerLock";
        ZookeeperLockNotifyImpl zookeeperLockNotify=new ZookeeperLockNotifyImpl();
        zkClient.registerLockNotify(Conver_zklock_converterSchedulerLockName,zookeeperLockNotify);
        System.out.println("pause");
        for(int i=0;i<3;i++){
            new Thread(()->{
                while (true){
                    zkClient.registerLockNotify(Conver_zklock_converterSchedulerLockName,zookeeperLockNotify);
                    Common.sleep(1000);
                    //刷新锁的心跳
//                    zkClient.updateLockHeart(Conver_zklock_converterSchedulerLockName);
                }
            },"线程"+i).start();
        }
    }
}
