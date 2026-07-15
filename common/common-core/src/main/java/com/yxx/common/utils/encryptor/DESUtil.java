package com.yxx.common.utils.encryptor;

import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Description:
 *
 * @author yxx
 * @since 2022-11-25
 */
public class DESUtil implements IEncryptor {
    /**
     * 偏移变量，固定占8位字节
     */
    private static final String IV_PARAMETER = "12345678";
    /**
     * 加密算法
     */
    private static final String ALGORITHM = "DES";
    /**
     * 加密/解密算法-工作模式-填充模式
     */
    private static final String CIPHER_ALGORITHM = "DES/CBC/PKCS5Padding";
    /**
     * 默认编码
     */
    private static final String CHARSET = String.valueOf(StandardCharsets.UTF_8);

    /**
     * 生成key
     *
     * @param password
     * @return
     * @throws Exception
     */
    private static Key generateKey(String password) {
        try {
            DESKeySpec dks = new DESKeySpec(password.getBytes(CHARSET));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            return keyFactory.generateSecret(dks);
        } catch (InvalidKeySpecException | InvalidKeyException | UnsupportedEncodingException |
                 NoSuchAlgorithmException e) {
            throw new ApiException(ApiCode.KEY_ERROR);
        }
    }


    /**
     * DES加密字符串
     *
     * @param key 加密密码，长度不能够小于8位
     * @param obj 待加密字符串
     * @return 加密后内容
     */
    @Override
    public String encrypt(Object obj, String key) {
        String data = obj.toString();
        if (key == null){
            return null;
        }
        if ( key.length() < 8) {
            throw new ApiException(ApiCode.KEY_LENGTH_ERROR);
        }
        try {
            Key secretKey = generateKey(key);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            IvParameterSpec iv = new IvParameterSpec(IV_PARAMETER.getBytes(CHARSET));
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

            // 加密
            byte[] bytes = cipher.doFinal(data.getBytes(CHARSET));

            // base64编码  JDK1.8及以上可直接使用Base64，JDK1.7及以下可以使用BASE64Encoder
            byte[] encode = Base64.getEncoder().encode(bytes);

            return new String(encode);

        } catch (Exception e) {
            e.printStackTrace();
            return data;
        }
    }

    /**
     * DES解密字符串
     *
     * @param key 解密密码，长度不能够小于8位
     * @param obj 待解密字符串
     * @return 解密后内容
     */
    @Override
    public String decrypt(Object obj, String key) {
        String data = obj.toString();
        if (key == null || key.length() < 8) {
            throw new ApiException(ApiCode.KEY_LENGTH_ERROR);
        }
        if (data == null)
            return null;
        try {
            Key secretKey = generateKey(key);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            IvParameterSpec iv = new IvParameterSpec(IV_PARAMETER.getBytes(CHARSET));
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

            // base64解码
            byte[] decode = Base64.getDecoder().decode(data.getBytes(CHARSET));

            // 解密
            byte[] decrypt = cipher.doFinal(decode);

            return new String(decrypt, CHARSET);
        } catch (Exception e) {
            e.printStackTrace();
            return data;
        }
    }

}