package cn.com.demo.controller;

import cn.com.demo.comm.BeanFactory;
import cn.com.demo.domain.OpTResult;
import cn.com.demo.domain.vo.HelloObject;
import cn.com.demo.service.HelloService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
public class HelloController {
    @Autowired
    private HelloService helloService;

    /**
     * 1、直接返回String,跳转到index.html
     * (要引入spring-boot-starter-thymeleaf)
     * @return
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index() {
        log.info("进入index界面");
        return "index";
    }


    // 异常页
//    @RequestMapping(value = "/error", method = RequestMethod.GET)
//    public String error() {
//        return "error";
//    }

    /**
     * 2、返回对象
     * @return
     */
    @RequestMapping(value = "/hello1", method = RequestMethod.GET)
    public OpTResult<HelloObject> sayHello1(){
        log.info("访问hello1");
        OpTResult<HelloObject> result=new OpTResult<>();
        try{
            result.setResult(helloService.sayHello());
        }catch (Exception e){
            result.setErrorInfo(e.getMessage(),e);
        }
        return result;
    }

    /**
     * 3、ResponseBody
     * @return
     */
    @RequestMapping(value = "/hello2", method = RequestMethod.GET)
    @ResponseBody
    public  OpTResult<HelloObject>  sayHello2(){
        log.info("访问hello2");
        OpTResult<HelloObject> result=new OpTResult<>();
        try{
            result.setResult(BeanFactory.getBean(HelloService.class).sayHello());
        }catch (Exception e){
            result.setErrorInfo(e.getMessage(),e);
        }
        return result;
    }

    /**
     * 4、有参RequestBody
     * @param helloObject
     * @return
     */
    @RequestMapping(value = "/hello3", method = RequestMethod.GET)
    @ResponseBody
    public  OpTResult<HelloObject>  sayHello3(@RequestBody HelloObject helloObject){
        log.info("访问hello3");
        OpTResult<HelloObject> result=new OpTResult<>();
        try{
            result.setResult(helloObject);
        }catch (Exception e){
            result.setErrorInfo(e.getMessage(),e);
        }
        return result;
    }

    /**
     *  有参RequestParam
     * @param name
     * @param type
     * @return
     */
    @RequestMapping(value = "/hello4", method = RequestMethod.GET)
    @ResponseBody
    public  OpTResult<HelloObject>  sayHello4(@RequestParam("name")String name,@RequestParam("type")String type){
        log.info("访问hello4");
        OpTResult<HelloObject> result=new OpTResult<>();
        try{
            result.setResult(new HelloObject(type,name));
        }catch (Exception e){
            result.setErrorInfo(e.getMessage(),e);
        }
        return result;
    }


    public String testAop(String key,String value){
        log.info("【Controller】运行了testAop,key="+key+",vale="+value);
        return "result1";
    }

}
