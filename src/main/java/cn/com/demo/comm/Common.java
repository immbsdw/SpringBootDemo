package cn.com.demo.comm;

import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.*;
import java.util.Date;
import java.util.List;

@Slf4j
public class Common {
    /**
     * 睡眠
     *
     * @param millis
     *            毫秒
     */
    public static void sleep(long millis) {

        try {
            Thread.sleep(millis);
        }
        catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 获取本地路径
     *
     * @param physicalPath
     *            physicalPath
     * @return String String
     */
    public static String getLocalPath(String physicalPath) {

        if (physicalPath == null) {
            return "";
        }

        boolean isStartWithDouble = false;
        if (File.separator.equalsIgnoreCase("\\")) {
            physicalPath = physicalPath.replace('/', '\\');
            isStartWithDouble = physicalPath.startsWith("\\\\");
        }
        else {
            physicalPath = physicalPath.replace('\\', '/');
        }
        StringBuffer sb = new StringBuffer();
        boolean isFit = false;
        for (int i = 0; i < physicalPath.length(); i++) {
            if (physicalPath.charAt(i) == File.separatorChar) {
                if (isFit) {
                    continue;
                }
                isFit = true;
            }
            else {
                isFit = false;
            }
            sb.append(physicalPath.charAt(i));
        }
        String sbStr = sb.toString();
        if (sbStr.endsWith(File.separator)) {
            sbStr = sbStr.substring(0, sbStr.length() - 1);
        }
        return (isStartWithDouble ? "\\" : "") + sbStr;
    }

    /**
     * 合并本地路径
     *
     * @param physicalPath
     *            physicalPath
     * @param args
     *            args
     * @return String String
     */
    public static String combineLocalPath(String physicalPath, String... args) {

        if (physicalPath == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        physicalPath = getLocalPath(physicalPath);
        while (physicalPath.endsWith(File.separator)) {
            physicalPath = physicalPath.substring(0, physicalPath.length() - 1);
        }
        sb.append(physicalPath);
        sb.append(File.separator);

        for (String s : args) {
            if (s == null || "".equalsIgnoreCase(s)) {
                continue;
            }
            s = getLocalPath(s);
            while (s.startsWith(File.separator)) {
                s = s.substring(1, s.length());
            }
            while (s.endsWith(File.separator)) {
                s = s.substring(0, s.length() - 1);
            }
            sb.append(s);
            sb.append(File.separator);
        }

        return sb.toString().substring(0, sb.length() - 1);
    }



    /**
     * 合并java路径
     *
     * @param physicalPath
     *            physicalPath
     * @param args
     *            args
     * @return String String
     */
    public static String combineJavaPath(String physicalPath, String... args) {

        if (physicalPath == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        physicalPath = physicalPath.replaceAll("\\\\","/");
        while (physicalPath.endsWith("/")) {
            physicalPath = physicalPath.substring(0, physicalPath.length() - 1);
        }
        sb.append(physicalPath);
        sb.append("/");
        for (String s : args) {
            if (s == null || "".equalsIgnoreCase(s)) {
                continue;
            }
            s = s.replaceAll("\\\\","/");
            while (s.startsWith("/")) {
                s = s.substring(1, s.length());
            }
            while (s.endsWith("/")) {
                s = s.substring(0, s.length() - 1);
            }
            sb.append(s);
            sb.append("/");
        }
        return sb.toString().substring(0, sb.length() - 1);
    }


    public static void main(String[] args) {
        System.out.println(combineJavaPath("/a","/","/c"));
        System.out.println(combineLocalPath("/a","/","/c"));
    }
    /**
     * 获取异常信息（带null处理）
     *
     * @param ex
     *            异常
     * @return String String
     */
    public static String getExceptionMsg(Exception ex) {

        if (ex == null) {
            return "";
        }
        return ex.toString();
    }

    /**
     * 获取Error信息（带null处理）
     *
     * @param ex
     *            异常
     * @return String String
     */
    public static String getErrorMsg(Error ex) {

        if (ex == null) {
            return "";
        }
        return ex.toString();
    }

    /**
     * @param src
     *            src
     * @param con
     *            con
     */
    public static void removeContain(List<Integer> src, List<Integer> con) {

        if (src == null || con == null) {
            return;
        }

        for (Object ob : con) {
            src.remove(ob);
        }
    }

    /**
     * 打印异常的详细信息，包含异常类型和堆栈信息
     * @param throwable 异常信息
     * @return String
     */
    public static String getStackTrace(Throwable throwable)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try
        {
            throwable.printStackTrace(pw);
            return sw.toString();
        } finally
        {
            pw.close();
        }
    }

    /**
     * Date转换为LocalDateTime
     * @param date
     * @return
     */
    public static LocalDateTime date2LocalDateTime(Date date){
        Instant instant = date.toInstant();//An instantaneous point on the time-line.(时间线上的一个瞬时点。)
        ZoneId zoneId = ZoneId.systemDefault();//A time-zone ID, such as {@code Europe/Paris}.(时区)
        return instant.atZone(zoneId).toLocalDateTime();
    }

    /**
     * localDateTime转换为Date
     * @param localDateTime
     * @return
     */
    public static Date localDateTime2Date(LocalDateTime localDateTime){
        if (localDateTime == null){
            return null;
        }
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zdt = localDateTime.atZone(zoneId);//Combines this date-time with a time-zone to create a  ZonedDateTime.
        return Date.from(zdt.toInstant());
    }

    /**
     * LocalDate转Date
     * @param localDate
     * @return
     */
    public static Date localDate2Date(LocalDate localDate) {
        if(null == localDate) {
            return null;
        }
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

}
