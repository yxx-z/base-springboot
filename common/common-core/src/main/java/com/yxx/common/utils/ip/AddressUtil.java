package com.yxx.common.utils.ip;


import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;

import java.util.Objects;

/**
 * 解析ip归属地
 *
 * @author yxx
 * @classname AddressUtil
 * @since 2023/07/24
 */
@Slf4j
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

    /**
     * 根据IP地址查询登录来源
     * 注：⚠️ 上线服务器时用这个方法，可以避免找不到ip2region.xdb文件,  上线时，在jar包所在的目录，放一份ip2region.xdb文件
     *
     * @param ip ip
     * @return {@link String }
     * @author yxx
     */
    /*public static String getCityInfo(String ip) {
        try {
            String path = System.getProperty("user.dir");
            FileInputStream fileInputStream = new FileInputStream(path + "/ip2region.xdb");
            Searcher searcher = Searcher.newWithBuffer(IoUtil.readBytes(fileInputStream));
            String search = searcher.search(ip);
            String[] split = search.split("\\|");
            return Arrays.toString(split);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }*/
}
