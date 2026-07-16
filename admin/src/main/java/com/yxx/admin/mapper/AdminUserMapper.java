package com.yxx.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yxx.admin.model.entity.AdminUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author yxx
 * @since 2022-11-12 13:57
 */
public interface AdminUserMapper extends BaseMapper<AdminUser> {

    /** 查询拥有指定角色且当前启用的管理员数量。 */
    @Select("""
            SELECT COUNT(DISTINCT au.id)
            FROM admin_user au
            JOIN rbac_subject_role aur
              ON aur.subject_type = 'admin' AND aur.subject_id = au.id
            JOIN rbac_role ar
              ON ar.id = aur.role_id AND ar.scope = 'admin'
            WHERE au.is_delete = 0
              AND au.status = 1
              AND ar.is_delete = 0
              AND ar.code = #{roleCode}
            """)
    long countActiveUsersByRoleCode(@Param("roleCode") String roleCode);

    /** 判断管理员是否拥有指定角色。 */
    @Select("""
            SELECT COUNT(1)
            FROM rbac_subject_role aur
            JOIN rbac_role ar
              ON ar.id = aur.role_id AND ar.scope = 'admin'
            WHERE aur.subject_type = 'admin'
              AND aur.subject_id = #{userId}
              AND ar.is_delete = 0
              AND ar.code = #{roleCode}
            """)
    long countUserRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);
}
