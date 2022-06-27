package cn.com.demo.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.time.LocalTime;

/**
 * 分布式锁的实现
 */
@Service
public class DistributeLockUtil {

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 获取分布式锁
     *
     * @param lockName   锁名称
     * @param expireTime 超时时间（秒）
     * @return
     */
    public Boolean getDistributeLock(String lockName, long expireTime) {
        while(true){
            LocalTime localTime = LocalTime.now();
            localTime=localTime.plusSeconds(expireTime);
            //1、通过setNx来获取锁
            boolean getSuccess = redisUtils.setNx(lockName, localTime);
            if (getSuccess) {
                //2.1、获取锁成功
                return true;
            }
            //2.2获取锁失败
            //3、判断是锁是否被正常持有
            //3.1获取锁的当前拥有者，持有锁的有效时间
            String vailTimeStr=(String)redisUtils.get(lockName);
            //重新获取锁发现已经被释放，重新开始运行
            if(vailTimeStr==null){
                continue;
            }
            LocalTime vailTime =LocalTime.parse(vailTimeStr);
            localTime = LocalTime.now();
            //3.2  判断有效时间是否在当前时间之后，在之后表明锁被其他线程正常持有，在之前说明锁的当前持有者发生了异常
            if (vailTime.isAfter(localTime)) {
                //被其他线程正常持有，当前线程获取锁失败
                return false;
            }
            //4、被其他线程异常持有,当前线程去重新获取，为了防止在当前线程获取的同时，其他线程先获取，使用getAndSet()来
            String oldTimeStr= (String)redisUtils.getSet(lockName,localTime.plusSeconds(expireTime));
            if(vailTimeStr.equals(oldTimeStr)){
                //没有其他线程在当前线程之前获取到锁
                return true;
            }
            //在当前线程获取锁之前，已经有其他线程获取到了锁
//            System.out.println("在当前线程获取锁之前，已经有其他线程获取到了锁");
            return false;
        }
    }

    /**
     * 释放锁
     * @param lockName
     */
    public void releaseDistributeLock(String lockName){
        redisUtils.remove(lockName);
    }
}
