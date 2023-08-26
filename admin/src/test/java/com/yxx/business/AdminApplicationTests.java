package com.yxx.business;

import cn.dev33.satoken.temp.SaTempUtil;
import com.yxx.admin.AdminApplication;
import com.yxx.admin.model.entity.AdminMenu;
import com.yxx.admin.model.entity.AdminUser;
import com.yxx.admin.service.AdminMenuService;
import com.yxx.common.utils.email.MailUtils;
import com.yxx.common.utils.redis.RedissonCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = AdminApplication.class)
class AdminApplicationTests {
    @Autowired
    private RedissonCache redissonCache;
    @Autowired
    private AdminMenuService menuService;
    @Autowired
    private MailUtils mailUtils;

    String key = "test";
    String userKey = "user";

    @Test
    void redisPutTest() {
        redissonCache.put(key, "测试呀");

        AdminUser user = new AdminUser();
        user.setLoginCode("120");
        user.setLoginName("护士");
        user.setPassword("120");
        redissonCache.put(userKey, user);
    }

    @Test
    void redisGetTest(){
        String string = redissonCache.getString(key);
        System.out.println("string = " + string);

        AdminUser value = redissonCache.get(userKey);
        System.out.println("user = " + value);
    }

    @Test
    void redisRemoveTest(){
        redissonCache.remove(key);
        redissonCache.remove(userKey);
    }

    @Test
    void menuTree(){
        List<AdminMenu> menus = menuService.menuTree();
        System.out.println("menus = " + menus);
    }

    @Test
    void mailTest() {
        mailUtils.baseSendMail("yangxx@88.com", "主题", "test", false);
    }

    @Test
    void tokenTest(){
        long timeout = SaTempUtil.getTimeout("asldfjaklsjfk");
        System.out.println("timeout = " + timeout);
    }

}
