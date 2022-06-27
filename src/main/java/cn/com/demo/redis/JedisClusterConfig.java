package cn.com.demo.redis;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class JedisClusterConfig{
    @Value("${spring.redis.cluster.nodes}")
    private String clusterNodes;

    @Value("${spring.redis.password}")
    private String password;

    @Value("${spring.redis.timeout}")
    private int timeout;

    @Value("${spring.redis.soTimeout}")
    private int soTimeout;

    @Value("${spring.redis.maxAttempts}")
    private int maxAttempts;

    @Value("${spring.redis.pool.max-idle}")
    private int maxIdle;

    @Value("${spring.redis.pool.min-idle}")
    private int minIdle;

    @Value("${spring.redis.pool.max-active}")
    private int maxActive;

    @Value("${spring.redis.pool.max-wait}")
    private long maxWait;

    //自动注入，根据JedisCluster中的信息生成
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;


    @Bean
    public JedisCluster getJedisCluster() {
        return new JedisCluster(getNodes(), timeout,soTimeout,maxAttempts,password,poolConfig());
    }

    @Bean
    public RedisTemplate<Object, Object> redisTemplate(){
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        //key
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        //设置value 自定义的序列化
        template.setDefaultSerializer(fastJsonRedisSerializer());
        //设置key序列化方式 String序列化
        template.setKeySerializer(stringRedisSerializer);
        return template;
    }

//    @Bean
//    @Override
//    public CacheManager cacheManager() {
//        // 初始化一个RedisCacheWriter
//        RedisCacheWriter cacheManager = RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);
//        // 设置默认过期时间：2 分钟
//        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
//                .entryTtl(Duration.ofMinutes(1))
//                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
//                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(fastJsonRedisSerializer()));
//        RedisCacheManager redisCacheManager = new RedisCacheManager(cacheManager, defaultCacheConfig);
//        return  redisCacheManager;
//    }
//
    public FastJsonRedisSerializer<Object> fastJsonRedisSerializer() {
        //允许反序列化
        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        fastJsonConfig.setSerializerFeatures(
                SerializerFeature.WriteNullListAsEmpty,
                SerializerFeature.WriteDateUseDateFormat,
                SerializerFeature.WriteEnumUsingToString,
                SerializerFeature.WriteClassName);
        FastJsonRedisSerializer<Object> fastJsonRedisSerializer = new FastJsonRedisSerializer<>(Object.class);
        fastJsonRedisSerializer.setFastJsonConfig(fastJsonConfig);
        return fastJsonRedisSerializer;
    }


    /**
     * 连接池配置
     * @return
     */
    private JedisPoolConfig poolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setMaxTotal(maxActive);
        config.setMaxWaitMillis(maxWait);
        return config;
    }

    /**
     * 获取集群节点数组
     * @return
     */
    private Set<HostAndPort> getNodes() {
        String[] cNodes = clusterNodes.split(",");
        Set<HostAndPort> nodes = new HashSet<HostAndPort>();
        // 分割出集群节点
        String[] hp;
        for (String node : cNodes) {
            hp = node.split(":");
            nodes.add(new HostAndPort(hp[0], Integer.parseInt(hp[1])));
        }
        return nodes;
    }
}



