package com.yxx.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yxx.admin.model.entity.ManagedBusinessUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 管理端访问业务用户表的数据适配器。 */
public interface ManagedBusinessUserMapper extends BaseMapper<ManagedBusinessUser> {

    /** 逻辑删除业务用户的全部登录身份，保留历史标识用于反复注销注册风控。 */
    @Update("""
            UPDATE user_identity
            SET is_delete = 1, update_time = NOW()
            WHERE user_id = #{userId} AND is_delete = 0
            """)
    int softDeleteIdentities(@Param("userId") Long userId);
}
