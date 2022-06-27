package cn.com.demo.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@Order(1)
public class AspectDemo {

    @Pointcut("execution(* cn.com.demo.controller.HelloController.testAop(String,String))")
    public void pointCut(){
        log.info("pointCut()运行");
    }

    @Around("pointCut()")
    public Object  doAround(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args=pjp.getArgs();
        log.info("【Around前半段】Key="+args[0]+"，value="+args[1]);
        args[0]="2";
        args[1]="B";
        Object result=pjp.proceed(args);
        log.info("【Around后半段】result="+result);
        return "result2";
    }


    @Before("pointCut()")
    public void doBefore(JoinPoint jp){
        Object[] args=jp.getArgs();
        log.info("【Before】Key="+args[0]+"，value="+args[1]);
    }


    @After("pointCut()")
    public void doAfter(){
        log.info("【After】运行doAfter");
    }

    @AfterReturning(pointcut = "pointCut()",returning="o")
    public void doAfterReturning(Object o){
        log.info("【AfterReturning】运行结果："+o);
    }

    @AfterThrowing(pointcut = "pointCut()",throwing="e")
    public void doAfterThrowing(Exception e){
        log.info("【AfterThrowing】运行结果："+e.getMessage());
    }
}
