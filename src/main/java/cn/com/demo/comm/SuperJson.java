package cn.com.demo.comm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Json
 */
@Slf4j
public class SuperJson {
    static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.writerWithDefaultPrettyPrinter();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(new JavaTimeModule());
    }

    private SuperJson() {
    }

    public static String toJSONString(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("序列化：{} 异常：{}", object.toString(), Common.getStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public static byte[] toJSONBytes(Object object) {
        try {
            return mapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            log.error("序列化：{} 异常：{}", object.toString(), Common.getStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String jsonString, Class<T> clazz) {
        try {
            return mapper.readValue(jsonString, clazz);
        } catch (IOException e) {
            log.error("反序列化：{} 异常：{}", clazz.getTypeName(), Common.getStackTrace(e));
            throw new RuntimeException(e);
        }
    }


    public static <T> List<T> parseList(String jsonString, Class<T> clazz) {
        try {
            JavaType javaType = mapper.getTypeFactory().constructParametricType(List.class, clazz);
            return mapper.readValue(jsonString, javaType);
        } catch (IOException e) {
            log.error("反序列化：{} 异常：{}", clazz.getTypeName(), Common.getStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String jsonString, Type clazz) {
        JavaType javaType = mapper.getTypeFactory().constructType(clazz);
        try {
            return mapper.readValue(jsonString, javaType);
        } catch (IOException e) {
            log.error("反序列化：{} 异常：{}", clazz.getTypeName(), Common.getStackTrace(e));
            throw new RuntimeException(e);
        }
    }

}
