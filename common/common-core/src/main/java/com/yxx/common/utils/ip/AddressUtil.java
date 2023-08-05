package com.yxx.common.utils.ip;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 解析ip属地
 *
 * @author yxx
 * @classname AddressUtil
 * @since 2023/07/28
 */
@Slf4j
@Component
public class AddressUtil {

    /**
     * 根据IP地址查询登录来源 线上用
     *
     * @param ip ip
     * @return {@link String }
     * @author yxx
     */
    public static String getCityInfo(String ip) {
        if (isIPv6Address(ip)) {
            return null;
        }
        if (!isValidIPv4(ip)) {
            return null;
        }

        try {
            String path = System.getProperty("user.dir");
            FileInputStream fileInputStream = new FileInputStream(path + "/ip2region.xdb");
            Searcher searcher = Searcher.newWithBuffer(IoUtil.readBytes(fileInputStream));
            return searcher.search(ip);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 得到ip归属地
     *
     * @return {@link String }
     * @author yxx
     */
    public static String getIpHomePlace(String requestIp, Integer num) {
        log.info("ip:{}", requestIp);
        String cityInfo = AddressUtil.getCityInfo(requestIp);
        log.info("地理位置信息：{}", cityInfo);
        String ipHomePlace = extractLocationInfo(cityInfo, num);
        log.info("解析后地址为:{}", ipHomePlace);
        return ipHomePlace;
    }

    /**
     * 提取位置信息
     *
     * @param ipLocation ip位置
     * @param dataType   数据类型
     * @return {@link String }
     * @author yxx
     */
    public static String extractLocationInfo(String ipLocation, int dataType) {
        if (CharSequenceUtil.isBlank(ipLocation)) {
            return "未知";
        }
        // 按照“|”符号将IP归属地拆分成数组
        String[] locationParts = ipLocation.split("\\|");

        // 获取省份和市区信息
        String province = locationParts[2];
        String city = locationParts[3];

        // 检查是否为内网IP
        if ("内网IP".equals(province) || "内网IP".equals(city)) {
            return "内网";
        }

        // 获取省份数据
        String result = "";
        if (dataType == 2) {
            result = removeSuffix(province);
        }
        // 获取市区数据，如果市区为空，则返回省份数据；如果省份数据也为空，则返回“未知”
        else if (dataType == 3) {
            result = removeSuffix(city);
            if ("0".equals(result) || "".equals(result)) {
                result = removeSuffix(province);
                if ("0".equals(result) || "".equals(result)) {
                    result = "未知";
                }
            }
        }
        return result;
    }

    /**
     * 辅助方法：去除字符串末尾的特定后缀
     *
     * @param str str
     * @return {@link String }
     * @author yxx
     */
    private static String removeSuffix(String str) {
        if (str.endsWith("省") || str.endsWith("市")) {
            return str.substring(0, str.length() - 1);
        }
        return str;
    }

    /**
     * 是有效ipv4
     *
     * @param ip ip地址
     * @return boolean
     * @author yxx
     */
    public static boolean isValidIPv4(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // 切分IP地址的四个部分
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        // 检查每个部分是否是有效的数字（0-255之间）
        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * ipv6地址是
     *
     * @param ipAddress ip地址
     * @return boolean
     * @author yxx
     */
    public static boolean isIPv6Address(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }

        try {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            return inetAddress instanceof java.net.Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }

}
