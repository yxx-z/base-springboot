package com.yxx.admin.model.request;

import com.yxx.common.core.page.BasePageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 管理端业务用户分页查询条件。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessUserPageReq extends BasePageRequest {

    /** 显示名称、手机号或邮箱的模糊检索关键词。 */
    private String keyword;

    /** 账号状态；为空时查询全部状态。 */
    private Boolean status;
}
