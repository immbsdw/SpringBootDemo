package cn.com.demo.domain.vo;

import lombok.Data;

@Data
public class HelloObject {
    public HelloObject(String type,String name){
        this.name=name;
        this.type=type;
    }
    private  String name;
    private  String type;
}
