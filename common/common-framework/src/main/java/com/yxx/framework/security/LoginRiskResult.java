package com.yxx.framework.security;

/**
 * 登录风险处理结果。
 *
 * @param metadataUpdateRequired 是否应更新最近登录元数据
 * @param ipRegion               本次登录 IP 归属地；仅在需要更新时使用
 */
public record LoginRiskResult(boolean metadataUpdateRequired, String ipRegion) {

    /** 风险能力关闭时明确表示“不更新”，避免使用 null 产生清空历史字段的歧义。 */
    public static LoginRiskResult noUpdate() {
        return new LoginRiskResult(false, null);
    }

    /** 风险能力开启时返回本次应保存的归属地。 */
    public static LoginRiskResult update(String ipRegion) {
        return new LoginRiskResult(true, ipRegion);
    }
}
