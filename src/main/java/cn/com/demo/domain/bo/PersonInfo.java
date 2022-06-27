package cn.com.demo.domain.bo;

import lombok.Data;

@Data
public class PersonInfo {
    public String name;
    public int age;
    public String city;

    public PersonInfo(){

    }

    public PersonInfo(String name,int age,String city){
        this.name=name;
        this.age=age;
        this.city=city;
    }

    @Override
    public String toString(){
        return "name->"+name+",age->"+age+",city->"+city;
    }
}
