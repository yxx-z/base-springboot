package com.yxx.common.utils.encryptor;

/**
 * @author yxx
 */
public interface IEncryptor {

    String encrypt(Object val2bEncrypted, String key);

    String decrypt(Object val2bDecrypted, String key);

}