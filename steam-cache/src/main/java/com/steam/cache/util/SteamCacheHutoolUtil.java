package com.steam.cache.util;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class SteamCacheHutoolUtil {
    private static String PRIVATE_KEY;
    private static RSA rsa;
    private static final String YONDIF_AMS_COMMON_DOMAIN = "yondif-ams-common";

    @Value("${steamCache.private_key:#{null}}")
    public void setPrivateKey(String privateKey) {
        if(StringUtils.isNotEmpty(privateKey)) {
            SteamCacheHutoolUtil.PRIVATE_KEY = privateKey;
            SteamCacheHutoolUtil.rsa = new RSA(SteamCacheHutoolUtil.PRIVATE_KEY, null);
        }
    }


    public static boolean verifyRSAContent(String content){
        Assert.hasLength(content,"content can not be null");
        Assert.notNull(rsa,"RSA init error,can not be null");

        byte[] aByte = HexUtil.decodeHex(content);
        byte[] decrypt = rsa.decrypt(aByte, KeyType.PrivateKey);
        String decryptContext = StrUtil.str(decrypt, CharsetUtil.CHARSET_UTF_8);
        return YONDIF_AMS_COMMON_DOMAIN.equals(decryptContext);
    }

    public static String lowerFirst(String str){
        char[] cArgs = str.toCharArray();
        cArgs[0]+=32;
        return String.valueOf(cArgs);
    }
}
