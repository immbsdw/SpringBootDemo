package cn.com.demo.domain.bo;

import lombok.Data;

import java.util.List;

@Data
public class Department {
    public Integer departmentId;
    public String departmentName;
    public List<PersonInfo> personInfo;
    public Department(){

    }

    public Department(Integer departmentId, String departmentName, List<PersonInfo> personInfo) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.personInfo = personInfo;
    }
}
