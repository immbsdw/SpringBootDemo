package cn.com.demo.redis;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 通过SETNX和lua脚本实现
 */
@Service
public class DistributeLockUtil2 {

    public final static String serverInstanceName="DemoServer1_";

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 上锁
     * @param lockName
     * @return
     */
    public Boolean getLock(String lockName,Long time,TimeUnit timeUnit){
        //1.通过setNX获取锁,Value为持有锁的线程名称
        Boolean result=redisUtils.setNxEx(lockName,serverInstanceName+Thread.currentThread().getName(),time, timeUnit);
        if(result){
            //setnx成功表示获取锁成功
            //TODO:添加定时任务去刷新锁的有效时间
        }
        return  result;
    }


    /**
     * 锁的状态
     * (1)锁不存在了,即任务已经超时了,无需释放锁,返回false
     * (2)锁仍然存在:
     *      (2.1)value还是当前的线程,正常运行,手动释放锁,返回true
     *      (2.2)value已经别为了其他线程,当前线程运行超时,锁被redis自动释放,不能释放锁,返回false
     * @param lockName
     * @return
     */
    public Boolean  releaseLock(String lockName){
        //1.获取lua脚本
        DefaultRedisScript<Boolean> redisScript=new DefaultRedisScript<>();
        redisScript.setResultType(Boolean.class);
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("redisLock.lua")));
        //2.执行lua脚本(原子),判断当前锁的
        List<String> keys=new ArrayList<>();
        keys.add(lockName);
        Boolean result=(Boolean)redisUtils.executeLua(redisScript,keys,DistributeLockUtil2.serverInstanceName+Thread.currentThread().getName());
        return result;
    }

}
