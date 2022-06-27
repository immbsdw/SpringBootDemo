package cn.com.demo.zookeeper.constant;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 线程池常量
 */
public class ThreadPoolConstant {


    /**
     * 节点监控线程池
     */
    public static final ExecutorService CONFIG_WATCH_THREAD_POOL = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("节点监控线程池");
            return thread;
        }
    });

    /**
     * 锁回调线程池
     */
    public static final ExecutorService LOCK_THREAD_POOL = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("锁回调线程池");
            return thread;
        }
    });

    /**
     * 锁检测线程池
     */
    public static final ExecutorService CHECK_THREAD_POOL = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("锁检测线程池");
            return thread;
        }
    });

    /**
     * 锁心跳线程池
     */
    public static final ExecutorService HEART_THREAD_POOL = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("锁心跳线程池");
            return thread;
        }
    });

}
