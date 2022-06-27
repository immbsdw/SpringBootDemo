package cn.com.demo.comm;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class BeanFactory implements ApplicationContextAware {

    private static ApplicationContext applicationContext=null;

    /**
     * Spring框架会调用该方法，从而让我能得到ApplicationContext对象
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if(BeanFactory.applicationContext==null){
            BeanFactory.applicationContext=applicationContext;
        }
    }

    /**
     * 获取applicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }


    /**
     * 通过名字获取Bean实例
     * @param name
     * @return
     */
    public static <T> T getBean(String name) {
        if (name == null) {
            return null;
        }
        return (T)(getApplicationContext().getBean(name));
    }

    /**
     *通过class获取Bean实例
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getBean(Class<T> clazz){
        return getApplicationContext().getBean(clazz);
    }


    /**
     * 通过name,以及Clazz返回指定的Bean
     * @param name
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return getApplicationContext().getBean(name, clazz);
    }
}
