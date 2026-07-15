package com.yxx.common.utils.encryptor;

/**
 * 旧版加密器兼容入口。
 *
 * <p>类名仅为兼容已有代码，实际实现已经迁移到 AES-GCM。新代码应直接使用
 * {@link AesGcmEncryptor}，不得再引入 DES 算法。</p>
 */
@Deprecated(since = "1.0.0", forRemoval = true)
public class DESUtil extends AesGcmEncryptor {
}
