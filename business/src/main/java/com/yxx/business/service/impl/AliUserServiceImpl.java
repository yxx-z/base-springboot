package com.yxx.business.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.AliUserMapper;
import com.yxx.business.model.entity.AliUser;
import com.yxx.business.model.request.AliQueryUserReq;
import com.yxx.business.model.response.AliUserIdRes;
import com.yxx.business.model.response.LoginRes;
import com.yxx.business.service.AliUserService;
import com.yxx.common.constant.LoginDevice;
import com.yxx.common.core.model.AliLoginUser;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.properties.AliProperties;
import com.yxx.common.utils.auth.AliLoginUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 阿里service实现类
 *
 * @author yxx
 * @classname AliServiceImpl
 * @since 2023-09-14 10:30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliUserServiceImpl extends ServiceImpl<AliUserMapper, AliUser> implements AliUserService {

    private final AliProperties aliProperties;

    /**
     * 授权码换取userId
     *
     * @param authCode 授权码
     * @return {@link AliUserIdRes }
     * @author yxx
     */
    @Override
    public AliUserIdRes getUserIdByAuthCode(String authCode) {
        //实例化客户端 参数：正式环境URL,Appid,商户私钥 PKCS8格式,字符编码格式,字符格式,支付宝公钥,签名方式
        AlipayClient alipayClient = new DefaultAlipayClient(aliProperties.getServerUrl(), aliProperties.getAppId(),
                aliProperties.getMerchantPrivateKey(), "json", aliProperties.getCharset(),
                aliProperties.getAlipayPublicKey(), aliProperties.getSignType()
        );
        AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
        // 值为authorization_code时，代表用code换取
        request.setGrantType("authorization_code");
        //授权码，用户对应用授权后得到的
        request.setCode(authCode);
        //这里使用execute方法
        AlipaySystemOauthTokenResponse response;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            log.error("调用支付宝获取userId接口失败，异常:{}", e.getErrMsg());
            throw new ApiException(ApiCode.SYSTEM_ERROR);
        }
        //刷新令牌，上次换取访问令牌时得到。见出参的refresh_token字段
        request.setRefreshToken(response.getAccessToken());
        //返回成功时 就将唯一标识返回
        if (response.isSuccess()) {
            log.info("调用成功");
            //我这里只返回了一个字段给前端用
            AliUserIdRes res = new AliUserIdRes();
            res.setUserId(response.getUserId());
            return res;
        } else {
            throw new ApiException(ApiCode.SYSTEM_ERROR);
        }
    }

    /**
     * 支付宝小程序登录
     *
     * @param req 请求参数
     * @return {@link LoginRes }
     * @author yxx
     */
    @Override
    public LoginRes aliLogin(AliQueryUserReq req) {
        // 根据支付宝小程序用户唯一标识查询用户是否在数据库中
        AliUser aliUserInfo = getOne(new LambdaQueryWrapper<AliUser>().eq(AliUser::getUserId, req.getUserId()));

        // 如果不存在
        if (ObjectUtil.isNull(aliUserInfo)) {
            // 新增到数据库中
            AliUser aliUser = new AliUser();
            BeanUtils.copyProperties(req, aliUser);
            save(aliUser);

            // 如果有类似会员功能，自行增加角色查询

            // 登录
            AliLoginUser aliLoginUser = new AliLoginUser();
            BeanUtils.copyProperties(aliUser, aliLoginUser);
            AliLoginUtils.login(aliLoginUser, LoginDevice.PC);

            // 返回token
            return new LoginRes(aliLoginUser.getToken());
        }

        // 如果已经存在
        // 登录
        AliLoginUser aliLoginUser = new AliLoginUser();
        // 如果有类似会员功能，自行增加角色查询
        BeanUtils.copyProperties(aliUserInfo, aliLoginUser);
        AliLoginUtils.login(aliLoginUser, LoginDevice.PC);

        // 返回token
        return new LoginRes(aliLoginUser.getToken());
    }

}
