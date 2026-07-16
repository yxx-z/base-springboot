package com.yxx.admin.model.request;

import com.yxx.common.core.page.BasePageRequest;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 管理员分页查询条件。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminUserPageReq extends BasePageRequest {

    /** 登录账号、显示名称、手机号或邮箱关键词。 */
    @Size(max = 100, message = "管理员查询关键词不能超过100位")
    private String keyword;

    /** 账号状态；为空时查询全部。 */
    private Boolean status;
}
