package cn.com.demo.comm;

import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.FileLock;

/**
 * 程序环境相关
 */
public class ProgramEnvironment {

    /**
     * 用于控制程序只能运行一次的锁对象
     */
    private static FileLock lock;

    /**
     * 判断当初程序是否重复运行
     *
     * @return
     */
    public static boolean singleRunChecker() {
        try {
            // 获得文件锁
            lock = new FileOutputStream("lock").getChannel().tryLock();
            // 返回空表示文件已被运行的实例锁定
            if (lock == null) {
                return true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return true;
        }
        return false;
    }

    /**
     * 获取当前程序运行的机器名
     *
     * @return
     */
    public static String getHostName() throws UnknownHostException {
        InetAddress addr = InetAddress.getLocalHost();
        return addr.getHostName();
    }


    /**
     * 获取项目所在路径(包括jar)
     *
     * @return
     */
    public static String getProjectPath(URL url) {
        String filePath = null;
        try {
            filePath = java.net.URLDecoder.decode(url.getPath(), "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (filePath.endsWith(".jar"))
            filePath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
        java.io.File file = new java.io.File(filePath);
        filePath = file.getAbsolutePath();
        return filePath;
    }
}
