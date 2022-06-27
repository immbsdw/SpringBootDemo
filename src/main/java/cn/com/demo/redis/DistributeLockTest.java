package cn.com.demo.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class DistributeLockTest {
    @Autowired
    private  DistributeLockUtil distributeLockUtil;
    @Autowired
    private  DistributeLockUtil2 distributeLockUtil2;

    public void run(){
        final String LOCK="myLock";
        for (int i=1;i<10;i++){
            int time=i;
            Thread thread=new Thread(()->{
                try {
                    doProcess2(LOCK,time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            },"线程"+time);
            thread.start();
        }
    }

    public void doProcess(String lockName,int time) throws InterruptedException {
        while (true){
            if(distributeLockUtil.getDistributeLock(lockName,5)){
               log.info(Thread.currentThread().getName()+"获取锁成功");
                Thread.sleep(time*1000);
                log.info(Thread.currentThread().getName()+"休眠后，释放锁");
                distributeLockUtil.releaseDistributeLock(lockName);
                return;
            }else{
                log.info(Thread.currentThread().getName()+"获取锁失败，短暂休眠后重新请求锁");
                Thread.sleep(1000);
            }
        }
    }

    public void doProcess2(String lockName,int time) throws InterruptedException {
        while (true){
            if(distributeLockUtil2.getLock(lockName,10l, TimeUnit.SECONDS)){
                log.info(Thread.currentThread().getName()+"获取锁成功");
                Thread.sleep(time*1000);
                log.info(Thread.currentThread().getName()+"休眠后，释放锁");
                distributeLockUtil2.releaseLock(lockName);
                return;
            }else{
                log.info(Thread.currentThread().getName()+"获取锁失败，短暂休眠后重新请求锁");
                Thread.sleep(1000);
            }
        }
    }
}
