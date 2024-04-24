package com.hspedu.seckill.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * @author yangda
 * @create 2024-04-21-19:31
 * @description: 生成uuid的工具类
 */
public class UUIDUtil {
    public static String uuid() {
        //把 UUID 中的- 替换掉
        return UUID.randomUUID().toString().replace("-", "");
    }



    @Test
    public void t1(){
        System.out.println("uuid= " + uuid());
    }

}
