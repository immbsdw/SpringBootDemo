package cn.com.demo.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@DependsOn("beanFactory")     // 优先加载工具类Common中
//不加此行则默认扫描Boot文件夹下的内容
@ComponentScan(value = "cn.com.demo")
@EnableAsync        // 开启异步注解
@EnableAspectJAutoProxy(proxyTargetClass=false,exposeProxy = true)     // 开启AOP
    public class SpringRun {

    private static ConfigurableApplicationContext run=null;
    public static void run() {
        if (run == null) {
            run = SpringApplication.run(SpringRun.class);
        }
    }
}
