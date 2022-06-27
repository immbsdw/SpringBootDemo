package cn.com.demo.zookeeper.domain;

import cn.com.demo.comm.Common;
import cn.com.demo.comm.ProgramEnvironment;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * 客户端信息
 */
@Data
public class ClientInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientInfo.class);

    private String clientId;

    private String hostName;

    private List<String> hostIpList;

    private String classLoader;

    public ClientInfo() {
        this.hostName = this.getHostName();
        this.classLoader = this.getClassPath();
        this.hostIpList = this.getHostIpList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClientInfo that = (ClientInfo) o;
        return classLoader.equals(that.classLoader) && (hostName.equals(that.hostName) || !Collections.disjoint(hostIpList, that.hostIpList));
    }

    /**
     * 获取计算机名
     */
    private String getHostName() {
        String hostName;
        try {
            hostName = ProgramEnvironment.getHostName();
        } catch (UnknownHostException e) {
            LOGGER.error("获取计算机名失败 {}", Common.getStackTrace(e));
            hostName = "UnknownHost";
        }
        return hostName;
    }

    /**
     * 获取程序运行路径
     */
    private String getClassPath() {
        return ProgramEnvironment.getProjectPath(ClientInfo.class.getProtectionDomain().getCodeSource().getLocation());
    }

    /**
     * 获取计算机IP
     */
    private List<String> getHostIpList() {
        List<String> ipList = new ArrayList<>();
        Enumeration allNetInterfaces;
        try {
            allNetInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            LOGGER.error("获取计算机IP失败 {}", Common.getStackTrace(e));
            return ipList;
        }
        InetAddress ip;
        while (allNetInterfaces.hasMoreElements()) {
            NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
            Enumeration addresses = netInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                ip = (InetAddress) addresses.nextElement();
                if (ip instanceof Inet4Address) {
                    ipList.add(ip.getHostAddress());
                    break;
                }
            }
        }
        return ipList;
    }

}
