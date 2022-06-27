package cn.com.demo;

import cn.com.demo.boot.SpringRun;
import cn.com.demo.comm.BeanFactory;
import cn.com.demo.controller.CachTestController;
import cn.com.demo.controller.HelloController;
import cn.com.demo.redis.DistributeLockTest;
import cn.com.demo.zookeeper.ZkClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {
    public static void main(String[] args) {
        System.out.println("It is master printing");
        System.out.println("It is hotFix printing22222");
        System.out.println("Push Test");
        SpringRun.run();
        log.info("启动成功");
        DistributeLockTest distributeLockTest=BeanFactory.getBean(DistributeLockTest.class);
        distributeLockTest.run();
//        HelloController helloController=BeanFactory.getBean(HelloController.class);
//        helloController.testAop("1","A");
//        CachTestController cachTestController=BeanFactory.getBean(CachTestController.class);
//        cachTestController.setDepartment();
//        cachTestController.getDepartment();
//        cachTestController.luaTest();
//        cachTestController.luaTest();
//        cachTestController.lockBylua();
//        cachTestController.lockBylua();
    }
}
