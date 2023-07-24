package com.yxx.common.utils.ip;


import org.lionsoul.ip2region.xdb.Searcher;

import java.util.Objects;

/**
 * 解析ip归属地
 *
 * @author yxx
 * @classname AddressUtil
 * @since 2023/07/24
 */
public class AddressUtil {

    /**
     * 根据IP地址查询登录来源
     *
     * @param ip ip
     * @return {@link String }
     * @author yxx
     */
    public static String getCityInfo(String ip) {
        try {
            // 获取当前记录地址位置的文件
            String dbPath = Objects.requireNonNull(AddressUtil.class.getResource("/ip2region/ip2region.xdb")).getPath();
            //创建查询对象
            Searcher searcher = Searcher.newWithFileOnly(dbPath);
            //开始查询
            return searcher.searchByStr(ip);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //默认返回空字符串
        return "";
    }
}
