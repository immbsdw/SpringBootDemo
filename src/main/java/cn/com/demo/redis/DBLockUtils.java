package cn.com.demo.redis;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class DBLockUtils {
    public ConcurrentHashMap<String, Lock> hotKeyLocks;

    public DBLockUtils(){
        hotKeyLocks=new ConcurrentHashMap<String,Lock>();
    }

    public Lock setNX(String key){
        //putIfAbsent
        //当key不存在插入成功返回null
        //当key存在插入失败返回原来的值
        Lock lock=hotKeyLocks.putIfAbsent(key,new ReentrantLock());
        if(lock==null){
            lock=hotKeyLocks.get(key);
        }
        return  lock;
    }
}
