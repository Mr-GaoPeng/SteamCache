package com.steam.cache.util;

import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AppMathUtil {

    //求多个字符串的乘积
    public static String multiply(String... str){
        //todo 借助转型Long来求乘积；遇到特别大的数值，在更改实现；
        Assert.isTrue(str.length != 1,"one number can not multiply");
        List<Long> longargs = Arrays.stream(str).map((data -> Long.parseLong(data))).collect(Collectors.toList());
        long temp = 1;
        for(Long ld : longargs){
            temp *=ld;
        }
        return String.valueOf(temp);
    }
}
