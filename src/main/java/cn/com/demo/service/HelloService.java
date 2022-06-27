package cn.com.demo.service;

import cn.com.demo.domain.vo.HelloObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class HelloService {

//    @Transactional(rollbackFor=Exception.class,propagation= Propagation.REQUIRED)
    public HelloObject sayHello(){
        log.info("运行sayHello");
        sayHello2();
        return new HelloObject("hi","Tom");
    }

    @Transactional(rollbackFor=Exception.class,propagation= Propagation.MANDATORY)
    public HelloObject sayHello2(){
        log.info("运行sayHello");
        return new HelloObject("hi","Tom");
    }
}
