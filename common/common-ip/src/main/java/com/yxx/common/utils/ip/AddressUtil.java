package com.yxx.common.utils.ip;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.IOException;
import java.io.InputStream;

/**
 * IP 归属地解析组件。
 *
 * <p>IP 数据库在组件初始化时从 classpath 加载一次，避免每个请求重复读取完整文件。
 * 归属地属于辅助信息，资源缺失或单次解析失败时仅记录告警并返回“未知”，不得阻断登录、
 * 审计等核心业务流程。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ip", name = "check", havingValue = "true")
public class AddressUtil {

    private static final String DATABASE_PATH = "ip2region/ip2region.xdb";
    private static final String UNKNOWN_LOCATION = "未知";

    private final Searcher searcher;

    public AddressUtil() {
        this.searcher = loadSearcher();
    }

    /**
     * 根据 IPv4 地址查询原始归属地数据。
     *
     * @param ip IPv4 地址
     * @return ip2region 原始结果；无法解析时返回 {@code null}
     */
    public String getCityInfo(String ip) {
        if (!IpUtil.isValidIPv4(ip) || searcher == null) {
            // 当前 xdb 仅用于 IPv4；资源加载失败时也直接降级，不阻断调用方。
            return null;
        }
        try {
            // ip2region 官方 Searcher 明确标注非线程安全，单例复用时必须串行访问。
            synchronized (searcher) {
                return searcher.search(ip);
            }
        } catch (Exception exception) {
            log.warn("解析 IP 归属地失败，ip={}", ip, exception);
            return null;
        }
    }

    /**
     * 获取省级或市级归属地。
     *
     * @param requestIp 客户端 IPv4 地址
     * @param dataType 2 表示省级，3 表示市级
     * @return 归属地名称；无法识别时返回“未知”
     */
    public String getIpHomePlace(String requestIp, int dataType) {
        return extractLocationInfo(getCityInfo(requestIp), dataType);
    }

    /**
     * 从 ip2region 的“国家|区域|省份|城市|ISP”结果中提取省份或城市。
     *
     * @param ipLocation ip2region 原始结果
     * @param dataType 2 表示省级，3 表示市级
     * @return 归属地名称
     */
    public String extractLocationInfo(String ipLocation, int dataType) {
        if (ipLocation == null || ipLocation.isBlank()) {
            return UNKNOWN_LOCATION;
        }
        // 使用 -1 保留末尾空字段，确保城市或 ISP 缺失时数组位置仍符合协议。
        String[] locationParts = ipLocation.split("\\|", -1);
        if (locationParts.length < 4) {
            log.warn("IP 归属地数据格式异常，value={}", ipLocation);
            return UNKNOWN_LOCATION;
        }

        String province = locationParts[2];
        String city = locationParts[3];
        if ("内网IP".equals(province) || "内网IP".equals(city)) {
            // 内网地址不伪装为未知公网地区，明确返回“内网”便于风险判断。
            return "内网";
        }
        if (dataType == 2) {
            return normalizeLocation(province);
        }
        if (dataType == 3) {
            String normalizedCity = normalizeLocation(city);
            // 城市缺失时退化到省级信息，尽可能返回可用归属地。
            return UNKNOWN_LOCATION.equals(normalizedCity)
                    ? normalizeLocation(province)
                    : normalizedCity;
        }
        log.warn("不支持的 IP 归属地数据类型，dataType={}", dataType);
        return UNKNOWN_LOCATION;
    }

    private Searcher loadSearcher() {
        try {
            // 资源可能位于可执行 Jar 内，必须通过 classpath 流读取，不能假设存在磁盘路径。
            ClassPathResource resource = new ClassPathResource(DATABASE_PATH);
            try (InputStream inputStream = resource.getInputStream()) {
                // 启动时一次性载入内存，避免每次查询重复进行文件 IO。
                return Searcher.newWithBuffer(inputStream.readAllBytes());
            }
        } catch (Exception exception) {
            log.error("加载 IP 归属地数据库失败，path={}；归属地能力将降级为未知", DATABASE_PATH, exception);
            return null;
        }
    }

    private String normalizeLocation(String location) {
        if (location == null || location.isBlank() || "0".equals(location)) {
            return UNKNOWN_LOCATION;
        }
        if (location.endsWith("省") || location.endsWith("市")) {
            // 去掉行政区后缀，使历史值与当前值比较时不受数据源展示格式影响。
            return location.substring(0, location.length() - 1);
        }
        return location;
    }

    /** 释放 ip2region 查询器持有的资源。 */
    @PreDestroy
    public void close() {
        if (searcher == null) {
            return;
        }
        try {
            searcher.close();
        } catch (IOException exception) {
            log.warn("关闭 IP 归属地查询器失败", exception);
        }
    }
}
