package cn.com.demo.domain;

import lombok.Data;

@Data
public class RedisObject<T> {
    private String key;
    private T value;
}
