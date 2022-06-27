package cn.com.demo.controller;

import cn.com.demo.domain.RedisObject;
import cn.com.demo.domain.bo.Department;
import cn.com.demo.domain.bo.PersonInfo;
import cn.com.demo.redis.DBLockUtils;
import cn.com.demo.redis.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Slf4j
@Controller
public class CachTestController {

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private DBLockUtils DBLockUtils;

    @RequestMapping(value = "/putRedis", method = RequestMethod.GET)
    @ResponseBody
    public void writeCache(@RequestBody RedisObject<PersonInfo> value) {
        redisUtils.set(value.getKey(), value.getValue(), 10L, TimeUnit.DAYS);
    }

    @RequestMapping(value = "/getFromRedis", method = RequestMethod.GET)
    @ResponseBody
    public Object readCache(@RequestParam("key") String key) {
        return redisUtils.get(key);
    }


    @RequestMapping(value = "/getPersonInfo", method = RequestMethod.GET)
    @ResponseBody
    public PersonInfo getPersonInfo(@RequestParam("name") String name) {
        return (PersonInfo) redisUtils.get(name);
    }


    public Department getDepartment() {
        Department department = (Department) redisUtils.get("Department_RD");
        return department;
    }


    public void setDepartment() {
        List<PersonInfo> personInfos = new ArrayList<>();
        personInfos.add(new PersonInfo("A", 1, "A"));
        personInfos.add(new PersonInfo("B", 2, "B"));
        personInfos.add(new PersonInfo("C", 3, "C"));
        Department department = new Department(1, "RD", personInfos);
        redisUtils.set("Department_RD", department, 10L, TimeUnit.DAYS);
    }

    public Object getByKey(String key) throws InterruptedException {
        //1.从redis中获取值
        Object result = redisUtils.get(key);
        if (result == null) {
            //2.redis中无查询数据,加锁查询数据库
            Lock lock = DBLockUtils.setNX(key);
            //是热key,上锁,双重检查锁
            if (lock.tryLock(5, TimeUnit.SECONDS)) {
                try{
                    //再从Redis读一次数据
                    result = redisUtils.get(key);
                    if (result != null) {
                        //TODO:(1)读取到数据则直接返回
                    } else {
                        //TODO:(2)还是未读取到数据,从数据库读取数据将读取到的数设置到redis中
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    lock.unlock();
                }
            }
        }
        return result;
    }


    public void luaTest(){
        //调用lua脚本并执行
        DefaultRedisScript<PersonInfo> redisScript = new DefaultRedisScript<PersonInfo>();
        //lua文件存放在resources目录下的redis文件夹内
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("redislua1.lua")));
        //设置返回值的类型,不设置会返回null
        redisScript.setResultType(PersonInfo.class);
        List<String> keys=new ArrayList<>();
        keys.add("LuaPersonInfo");
        PersonInfo defaultPerson=new PersonInfo("DefaultPerson",20,"Shanghai");
        PersonInfo personInfo1=new PersonInfo("Tom",10,"Beijing");
        Object result=redisUtils.executeLua(redisScript,keys,defaultPerson,personInfo1);
        if(result!=null){
            log.info(result.toString());
        }
    }

    public void luaTest2(){
        //调用lua脚本并执行
        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<Boolean>();
        //lua文件存放在resources目录下的redis文件夹内
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("redislua1.lua")));
        //设置返回值的类型,不设置会返回null
        redisScript.setResultType(Boolean.class);
        List<String> keys=new ArrayList<>();
        keys.add("LuaTest");
        Object result=redisUtils.executeLua(redisScript,keys);
        if(result!=null){
            log.info(result.toString());
        }
    }


    public Long lockBylua(){
        //调用lua脚本并执行
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<Long>();
        //返回类型是Long
        redisScript.setResultType(Long.class);
        //lua文件存放在resources目录下的redis文件夹内
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("redisLock.lua")));
        List<String> keys=new ArrayList<>();
        keys.add("luaLock");
        Object result=redisUtils.executeLua(redisScript,keys,"Thread1",100);
        return (Long)result;
    }
}
